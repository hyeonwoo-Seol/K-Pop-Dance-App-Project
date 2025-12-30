# >> scoring.py

import json
import numpy as np
import os
from scipy.spatial.distance import euclidean
from fastdtw import fastdtw

class ScoringCalculator:
    def __init__(self):
        # YOLO v11 Keypoint Index Mapping
        # 0:Nose, 1:LEye, 2:REye, 3:LEar, 4:REar, 5:LShoulder, 6:RShoulder
        # 7:LElbow, 8:RElbow, 9:LWrist, 10:RWrist
        # 11:LHip, 12:RHip, 13:LKnee, 14:RKnee, 15:LAnkle, 16:RAnkle
        
        # 분석에 사용할 주요 관절 연결부 (벡터 생성용)
        # (Start Index, End Index)
        self.bones = [
            (5, 7), (7, 9),    # Left Arm (Shoulder->Elbow->Wrist)
            (6, 8), (8, 10),   # Right Arm
            (11, 13), (13, 15), # Left Leg (Hip->Knee->Ankle)
            (12, 14), (14, 16), # Right Leg
            (5, 6),            # Shoulders (Structure)
            (11, 12),          # Hips (Structure)
            (5, 11), (6, 12)   # Torso (Shoulder->Hip)
        ]

        # 오차 원인 분석을 위한 신체 부위 그룹핑
        self.body_parts = {
            "Left Arm": [(5, 7), (7, 9)],
            "Right Arm": [(6, 8), (8, 10)],
            "Left Leg": [(11, 13), (13, 15)],
            "Right Leg": [(12, 14), (14, 16)],
            "Torso": [(5, 11), (6, 12), (5, 6), (11, 12)]
        }

    def load_keypoints(self, json_path):
        """JSON 파일에서 프레임별 키포인트 데이터(x, y)만 추출합니다."""
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        # frames 리스트에서 keypoints만 추출 (3번째 값인 confidence 제외하고 x,y만)
        # shape: (num_frames, 17, 2)
        keypoints_sequence = []
        for frame in data.get('frames', []):
            if not frame.get('keypoints'):
                continue
            
            # YOLO 출력은 [x, y, conf] 형태이므로 x, y만 슬라이싱
            kps = np.array(frame['keypoints'])[:, :2]
            keypoints_sequence.append(kps)
            
        return np.array(keypoints_sequence), data

    def normalize_pose(self, keypoints_seq):
        """
        [Step 2] 데이터 정규화 (Normalization)
        1. Translation: 골반 중심(Pelvis)을 (0,0)으로 이동
        2. Scaling: 목(Neck)부터 발목 중점(Mid-Ankle)까지의 거리를 1로 맞춤
        """
        normalized_seq = []
        
        for kps in keypoints_seq:
            # 1. 골반 중심(Pelvis) 계산: (Left Hip + Right Hip) / 2
            left_hip = kps[11]
            right_hip = kps[12]
            pelvis = (left_hip + right_hip) / 2.0
            
            # 모든 관절을 골반 중심으로 이동 (Translation)
            translated_kps = kps - pelvis
            
            # 2. 크기 보정 (Scaling)
            # Neck 계산: (Left Shoulder + Right Shoulder) / 2 (YOLO엔 Neck이 없으므로 계산)
            left_shoulder = translated_kps[5]
            right_shoulder = translated_kps[6]
            neck = (left_shoulder + right_shoulder) / 2.0
            
            # Mid-Ankle 계산
            left_ankle = translated_kps[15]
            right_ankle = translated_kps[16]
            mid_ankle = (left_ankle + right_ankle) / 2.0
            
            # 키(Height) 계산: 목에서 발목 사이의 거리
            # 만약 거리가 0에 가까우면(노이즈) 1로 설정하여 나눗셈 에러 방지
            height = np.linalg.norm(neck - mid_ankle)
            scale_factor = 1.0 / height if height > 1e-6 else 1.0
            
            # 스케일링 적용
            scaled_kps = translated_kps * scale_factor
            normalized_seq.append(scaled_kps)
            
        return np.array(normalized_seq)

    def extract_feature_vectors(self, keypoints_seq):
        """
        좌표 자체보다는 '관절의 방향(벡터)'을 비교하는 것이 정확합니다.
        각 뼈(Bone)의 단위 벡터(Unit Vector)를 추출합니다.
        """
        feature_seq = []
        
        for kps in keypoints_seq:
            frame_vectors = []
            for (start_idx, end_idx) in self.bones:
                # 벡터 계산: End - Start
                vec = kps[end_idx] - kps[start_idx]
                
                # 단위 벡터로 변환 (크기 1, 방향만 남김)
                norm = np.linalg.norm(vec)
                unit_vec = vec / norm if norm > 1e-6 else vec * 0
                
                # (x, y) 성분을 평탄화하여 추가
                frame_vectors.extend(unit_vec)
            
            feature_seq.append(frame_vectors)
            
        return np.array(feature_seq) # Shape: (Frames, Num_Bones * 2)

    def calculate_score(self, user_json_path, expert_json_path):
        """
        [Step 3 ~ 4] DTW 알고리즘 적용 및 점수 산출
        """
        print(f"📊 [Scoring] 채점 시작: {os.path.basename(user_json_path)}")

        # 1. 데이터 로드
        user_kps, user_full_data = self.load_keypoints(user_json_path)
        expert_kps, _ = self.load_keypoints(expert_json_path)
        
        if len(user_kps) == 0 or len(expert_kps) == 0:
            print("❌ [Scoring] 유효한 키포인트 데이터가 없습니다.")
            return None

        # 2. 정규화 (Normalization)
        user_norm = self.normalize_pose(user_kps)
        expert_norm = self.normalize_pose(expert_kps)
        
        # 3. 특징 벡터 추출 (Vectorization)
        # 좌표 대신 뼈의 방향 벡터를 사용하여 비교 (체형 차이 극복)
        user_features = self.extract_feature_vectors(user_norm)
        expert_features = self.extract_feature_vectors(expert_norm)

        # 4. DTW 알고리즘 적용
        # dist: 사용자와 전문가의 동작 거리(비유사도)
        # path: [(user_frame_idx, expert_frame_idx), ...] 매핑 경로
        distance, path = fastdtw(user_features, expert_features, dist=euclidean)
        
        # 5. 점수 변환 (Scoring)
        # 평균 프레임 거리 계산
        avg_dist = distance / len(path)
        
        # 지수 함수 매핑: Score = 100 * exp(-avg_dist / alpha)
        # alpha 값은 관대함의 정도. (테스트를 통해 조절 필요. 일단 1.5로 설정)
        alpha = 1.5
        final_score = 100 * np.exp(-avg_dist / alpha)
        final_score = max(0, min(100, final_score)) # 0~100 사이로 클램핑

        # 6. 최다 오류 구간 및 부위 분석
        worst_parts_counter = {}
        timeline_feedback = []
        
        # DTW 경로를 따라가며 프레임별 상세 분석
        # path 샘플링 (너무 많으므로 5프레임마다 분석)
        for u_idx, e_idx in path[::5]: 
            u_vec = user_norm[u_idx] # 정규화된 좌표 사용
            e_vec = expert_norm[e_idx]
            
            frame_errors = {}
            total_frame_error = 0
            
            # 부위별 오차 계산
            for part_name, indices_list in self.body_parts.items():
                part_error = 0
                for (start, end) in indices_list:
                    # 해당 뼈의 벡터 차이 계산
                    u_bone = u_vec[end] - u_vec[start]
                    e_bone = e_vec[end] - e_vec[start]
                    part_error += euclidean(u_bone, e_bone)
                
                frame_errors[part_name] = part_error
                total_frame_error += part_error
                
            # 가장 많이 틀린 부위 찾기
            worst_part = max(frame_errors, key=frame_errors.get)
            worst_parts_counter[worst_part] = worst_parts_counter.get(worst_part, 0) + 1
            
            # 타임라인 기록 (오차가 임계값 이상일 때만)
            if total_frame_error > 0.5: # 임계값 설정
                # 프레임 인덱스를 시간(초)으로 변환 (30fps 가정)
                timestamp = round(u_idx / 30.0, 2)
                timeline_feedback.append({
                    "time": timestamp,
                    "worst_part": worst_part,
                    "error_val": round(total_frame_error, 2)
                })

        # 가장 많이 틀린 부위 Top 1
        most_wrong_body_part = max(worst_parts_counter, key=worst_parts_counter.get) if worst_parts_counter else "None"

        # 7. 결과 통합
        result_summary = {
            "score": round(final_score, 1),
            "grade": self.get_grade(final_score),
            "worst_part": most_wrong_body_part,
            "timeline": timeline_feedback
        }
        
        # 원본 데이터에 summary 추가하여 저장할 준비
        user_full_data["scoring_result"] = result_summary
        
        print(f"✅ [Scoring] 점수: {result_summary['score']}점 (Grade: {result_summary['grade']})")
        
        return user_full_data

    def get_grade(self, score):
        if score >= 90: return "S"
        elif score >= 80: return "A"
        elif score >= 70: return "B"
        elif score >= 60: return "C"
        else: return "D"

# 테스트 실행용
if __name__ == "__main__":
    scorer = ScoringCalculator()
    # scorer.calculate_score("data/analyzed_json/user_test.json", "data/analyzed_json/expert_test.json")
