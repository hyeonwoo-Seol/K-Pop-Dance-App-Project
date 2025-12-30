# >> scoring.py
# >> 사용자의 스켈레톤 데이터를 정규화하고, 전문가 데이터와 비교하여 점수를 산출한다.
# >> DTW 알고리즘을 사용하여 시간 축의 차이를 보정하고, 코사인 유사도(각도) 기반으로 정확도를 평가한다.

import numpy as np
import json
import math
import os
from scipy.spatial.distance import euclidean

# fastdtw가 설치되어 있으면 사용하고, 없으면 scipy cdist를 이용한 자체 구현이나 경고를 띄운다.
try:
    from fastdtw import fastdtw
except ImportError:
    fastdtw = None
    print("[Warning] fastdtw 라이브러리가 없습니다. DTW 성능이 저하될 수 있습니다.")

class Scoring:
    # >> 관절 인덱스 매핑 (YOLO v11 기준)
    # 0:Nose, 1:L-Eye, 2:R-Eye, 3:L-Ear, 4:R-Ear
    # 5:L-Shoulder, 6:R-Shoulder, 7:L-Elbow, 8:R-Elbow, 9:L-Wrist, 10:R-Wrist
    # 11:L-Hip, 12:R-Hip, 13:L-Knee, 14:R-Knee, 15:L-Ankle, 16:R-Ankle, 17:Neck(Custom)
    
    # >> 분석할 주요 관절 각도 정의 (3점: A-B-C에서 B의 각도)
    ANGLES_DEF = {
        "left_elbow": (5, 7, 9),      # L-Shoulder - L-Elbow - L-Wrist
        "right_elbow": (6, 8, 10),    # R-Shoulder - R-Elbow - R-Wrist
        "left_shoulder": (11, 5, 7),  # L-Hip - L-Shoulder - L-Elbow
        "right_shoulder": (12, 6, 8), # R-Hip - R-Shoulder - R-Elbow
        "left_knee": (11, 13, 15),    # L-Hip - L-Knee - L-Ankle
        "right_knee": (12, 14, 16),   # R-Hip - R-Knee - R-Ankle
        "left_hip": (5, 11, 13),      # L-Shoulder - L-Hip - L-Knee
        "right_hip": (6, 12, 14)      # R-Shoulder - R-Hip - R-Knee
    }

    # >> 부위별 인덱스 매핑 (Worst Part 분석용)
    BODY_PARTS = {
        "Left Arm": ["left_elbow", "left_shoulder"],
        "Right Arm": ["right_elbow", "right_shoulder"],
        "Left Leg": ["left_knee", "left_hip"],
        "Right Leg": ["right_knee", "right_hip"]
    }

    def __init__(self):
        pass

    # >> 메인 비교 함수
    def compare(self, user_json_path, expert_json_path):
        print(f"[Scoring] 점수 계산 시작: User({os.path.basename(user_json_path)}) vs Expert({os.path.basename(expert_json_path)})")

        # 1. 데이터 로드
        user_data = self._load_json(user_json_path)
        expert_data = self._load_json(expert_json_path)
        
        if not user_data or not expert_data:
            raise ValueError("데이터 로드 실패")

        # 2. 프레임별 키포인트 추출 (유효한 프레임만)
        user_frames = [f["keypoints"] for f in user_data["frames"] if f["is_valid"]]
        expert_frames = [f["keypoints"] for f in expert_data["frames"] if f["is_valid"]]

        if len(user_frames) == 0 or len(expert_frames) == 0:
            print("[Scoring] 유효한 프레임이 부족하여 점수를 계산할 수 없습니다.")
            return None

        # 3. 데이터 정규화 (Normalization)
        user_norm = [self._normalize_skeleton(f) for f in user_frames]
        expert_norm = [self._normalize_skeleton(f) for f in expert_frames]

        # 4. 특징 벡터 추출 (각도 계산)
        user_features = [self._extract_angles(f) for f in user_norm]
        expert_features = [self._extract_angles(f) for f in expert_norm]
        
        # 5. DTW 알고리즘 적용
        distance, path = self._calculate_dtw(user_features, expert_features)
        
        # 6. 점수 변환 (Scoring)
        final_score = self._convert_distance_to_score(distance, len(path))
        
        # 7. 취약 부위, 잘한 부위, 타임라인 분석 및 프레임별 점수 계산
        worst_part, best_part, timeline, frame_scores = self._analyze_errors(path, user_features, expert_features, user_data["frames"])

        print(f"[Scoring] 분석 완료 - 점수: {final_score:.1f}, 취약 부위: {worst_part}, 잘한 부위: {best_part}")

        return {
            "total_score": int(final_score),
            "worst_part": worst_part,
            "best_part": best_part,
            "timeline": timeline,
            "frame_scores": frame_scores
        }

    # >> JSON 파일 로드 헬퍼
    def _load_json(self, path):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"[Error] JSON 로드 실패 ({path}): {e}")
            return None

    # >> 스켈레톤 정규화 (Step 2)
    # >> 1. Translation: 골반(Hips) 중심을 (0,0)으로 이동
    # >> 2. Scaling: 목(Neck)부터 발목(Ankle) 중점까지의 길이를 1로 맞춤
    def _normalize_skeleton(self, keypoints):
        # keypoints format: [[x, y, conf], ...]
        kp = np.array(keypoints)[:, :2] # 좌표만 사용 (conf 제외)
        
        # 1. 중심점 계산 (Left Hip: 11, Right Hip: 12)
        hip_center = (kp[11] + kp[12]) / 2.0
        
        # Translation
        kp -= hip_center
        
        # 2. 스케일링 기준 길이 계산 (Neck: 17, Ankles: 15, 16)
        # Neck 좌표가 없으면(0,0) 어깨 중점 사용
        neck = kp[17]
        ankle_center = (kp[15] + kp[16]) / 2.0
        
        height = np.linalg.norm(neck - ankle_center)
        
        # 예외 처리: 높이가 너무 작거나 0이면 스케일링 건너뜀 (ZeroDivision 방지)
        if height < 0.05: 
            scale_factor = 1.0
        else:
            scale_factor = 1.0 / height
            
        return kp * scale_factor

    # >> 각도 특징 추출 (Step 3 보완)
    # >> 좌표값 직접 비교보다 각도가 체형 차이에 강건함
    def _extract_angles(self, keypoints):
        angles = []
        for name, indices in self.ANGLES_DEF.items():
            idx_a, idx_b, idx_c = indices
            
            # 각 키포인트 가져오기
            a = keypoints[idx_a]
            b = keypoints[idx_b]
            c = keypoints[idx_c]
            
            # 각도 계산
            angle = self._calculate_angle_3points(a, b, c)
            angles.append(angle)
            
        return np.array(angles)

    # >> 세 점 사이의 각도 계산 (0 ~ 180도)
    def _calculate_angle_3points(self, a, b, c):
        ba = a - b
        bc = c - b
        
        cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc) + 1e-6)
        angle = np.arccos(np.clip(cosine_angle, -1.0, 1.0))
        
        return np.degrees(angle)

    # >> DTW 거리 계산 (Step 3)
    def _calculate_dtw(self, user_seq, expert_seq):
        if fastdtw:
            # fastdtw 사용 (추천)
            distance, path = fastdtw(user_seq, expert_seq, dist=euclidean)
        else:
            # fastdtw가 없으면 단순 유클리드 거리 합 (Fallback, 길이가 같다고 가정하거나 간단히 처리)
            # 여기서는 길이가 다르면 잘라서 비교하는 단순 로직 적용 (임시)
            min_len = min(len(user_seq), len(expert_seq))
            u = np.array(user_seq)[:min_len]
            e = np.array(expert_seq)[:min_len]
            distance = np.sum([euclidean(u[i], e[i]) for i in range(min_len)])
            path = list(zip(range(min_len), range(min_len)))
            
        return distance, path

    # >> 점수 변환 (Step 4)
    # >> 지수 함수(Exponential Decay)를 사용하여 거리가 멀어질수록 점수 급감
    def _convert_distance_to_score(self, total_distance, path_length):
        if path_length == 0: return 0
        
        avg_distance = total_distance / path_length
        
        # Alpha 값은 관대함의 정도. 값이 클수록 관대함.
        # 각도 기반이므로 평균 오차가 10도일 때 약 80점이 나오도록 튜닝 필요
        # 예: avg_dist = 10 -> exp(-10/alpha) = 0.8 -> -10/alpha = ln(0.8) -> alpha = -10/ln(0.8) ~= 45
        alpha = 45.0 
        
        score = 100 * np.exp(-avg_distance / alpha)
        return max(0, min(100, score))

    # >> 취약 부위 및 구간별 피드백 생성 (Step 4-1)
    def _analyze_errors(self, path, user_features, expert_features, raw_frames):
        # 부위별 누적 오차
        part_errors = {name: 0.0 for name in self.BODY_PARTS.keys()}
        angle_names = list(self.ANGLES_DEF.keys())
        
        timeline = []
        frame_scores = [0.0] * len(raw_frames)
        
        # DTW 경로를 따라 오차 분석
        for u_idx, e_idx in path:
            u_vec = user_features[u_idx]
            e_vec = expert_features[e_idx]
            
            # 각 관절(각도)별 차이 계산
            diffs = np.abs(u_vec - e_vec)
            mean_diff = np.mean(diffs)
            
            # 부위별로 오차 합산
            for part_name, angles_in_part in self.BODY_PARTS.items():
                for angle_name in angles_in_part:
                    idx = angle_names.index(angle_name)
                    part_errors[part_name] += diffs[idx]
            
            # 프레임별 점수 기록
            if u_idx < len(raw_frames):
                frame_score = self._convert_distance_to_score(mean_diff, 1)
                frame_scores[u_idx] = float(f"{frame_score:.1f}")

                frame_info = raw_frames[u_idx]
                
                # 타임라인 생성 (1초 단위로 샘플링하여 구간 정보 생성)
                # 규격서 요구사항: start_sec, end_sec, score, error_part_index
                if frame_info['frame_index'] % 30 == 0: # 30프레임마다 기록 (약 1초 간격)
                    start_sec = frame_info['timestamp']
                    end_sec = start_sec + 1.0 # 1초 구간으로 설정
                    
                    timeline.append({
                        "start_sec": float(f"{start_sec:.2f}"),
                        "end_sec": float(f"{end_sec:.2f}"),
                        "score": int(frame_score),
                        "comment": "자세 정확도가 떨어집니다.", # 기본 코멘트
                        "error_part_index": [i for i, d in enumerate(diffs) if d > 15.0] # 15도 이상 차이나는 인덱스
                    })

        # 가장 많이 틀린 부위 찾기 (오차가 가장 큰 부위)
        worst_part = max(part_errors, key=part_errors.get)
        
        # 가장 잘한 부위 찾기 (오차가 가장 작은 부위)
        best_part = min(part_errors, key=part_errors.get)
        
        return worst_part, best_part, timeline, frame_scores
