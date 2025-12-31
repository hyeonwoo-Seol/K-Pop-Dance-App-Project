# >> scoring.py
# >> 사용자의 스켈레톤 데이터를 정규화하고, 전문가 데이터와 비교하여 점수를 산출한다.
# >> DTW 알고리즘을 사용하여 시간 축의 차이를 보정하고, 코사인 유사도(각도) 기반으로 정확도를 평가한다.

import numpy as np
import json
import math
import os
from scipy.spatial.distance import euclidean
from fastdtw import fastdtw

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
        # 개선사항2 적용: 전역 각도(몸통 기울기)가 추가된 특징 벡터를 추출한다.
        user_features = [self._extract_angles(f) for f in user_norm]
        expert_features = [self._extract_angles(f) for f in expert_norm]
        
        # 5. DTW 알고리즘 적용
        distance, path = self._calculate_dtw(user_features, expert_features)
        
        # 6. 점수 변환 (Scoring)
        # 개선사항1 적용: 모양 점수와 타이밍 점수를 가중 합산한다.
        
        # 6-1. 모양 점수 (Shape Score) - 동작의 정확도
        shape_score = self._convert_distance_to_score(distance, len(path))
        
        # 6-2. 타이밍 점수 (Timing Score) - 박자의 정확도
        timing_score = self._calculate_timing_score(path)
        
        # 6-3. 최종 점수 (Weighted Sum)
        # 구현 아이디어: 모양 점수 * 0.8 + 타이밍 점수 * 0.2
        final_score = (shape_score * 0.8) + (timing_score * 0.2)
        
        print(f"[Scoring] 세부 점수 - Shape: {shape_score:.1f}, Timing: {timing_score:.1f} -> Final: {final_score:.1f}")
        
        # 7. 취약 부위, 잘한 부위, 타임라인 분석 및 프레임별 점수 계산
        worst_part, best_part, timeline, frame_scores = self._analyze_errors(path, user_features, expert_features, user_data["frames"])

        print(f"[Scoring] 분석 완료 - 종합 점수: {final_score:.1f}, 취약 부위: {worst_part}")

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
    # >> 2. Scaling: 척추 길이(Neck ~ Hip Center)를 1로 맞춤
    def _normalize_skeleton(self, keypoints):
        # keypoints format: [[x, y, conf], ...]
        kp = np.array(keypoints)[:, :2] # 좌표만 사용 (conf 제외)
        
        # 1. 중심점 계산 (Left Hip: 11, Right Hip: 12)
        hip_center = (kp[11] + kp[12]) / 2.0
        
        # Translation
        kp -= hip_center
        
        # 2. 스케일링 기준 길이 계산 (척추 길이 기준)
        # Neck: 17
        neck = kp[17]
        
        # 수정: 골반 중심(척추 길이) 기준 사용
        height = np.linalg.norm(neck - hip_center)
        
        # 예외 처리: 높이가 너무 작거나 0이면 스케일링 건너뜀 (ZeroDivision 방지)
        if height < 0.05: 
            scale_factor = 1.0
        else:
            scale_factor = 1.0 / height
            
        return kp * scale_factor

    # >> 각도 특징 추출 (Step 3 보완)
    # >> 개선사항2: 전역 각도(Global Angle) 추가
    def _extract_angles(self, keypoints):
        angles = []
        # 1. 기존 내부 관절 각도 (8개)
        for name, indices in self.ANGLES_DEF.items():
            idx_a, idx_b, idx_c = indices
            a = keypoints[idx_a]
            b = keypoints[idx_b]
            c = keypoints[idx_c]
            angle = self._calculate_angle_3points(a, b, c)
            angles.append(angle)
            
        # 2. 전역 각도 (Torso Angle) 추가
        # 몸통이 수직선(Vertical)에서 얼마나 기울어졌는지 측정
        # 정규화된 데이터에서 Hips는 (0,0)이므로 Neck(17)의 좌표 자체가 척추 벡터임
        neck_vec = keypoints[17] 
        vertical_vec = np.array([0, -1]) # 이미지 좌표계에서 위쪽 방향 (Y 감소 방향)
        
        torso_angle = self._calculate_angle_vector(neck_vec, vertical_vec)
        angles.append(torso_angle)
            
        return np.array(angles)

    # >> 세 점 사이의 각도 계산 (0 ~ 180도)
    def _calculate_angle_3points(self, a, b, c):
        ba = a - b
        bc = c - b
        return self._calculate_angle_vector(ba, bc)

    # >> 두 벡터 사이의 각도 계산 (Helper)
    def _calculate_angle_vector(self, v1, v2):
        norm_v1 = np.linalg.norm(v1)
        norm_v2 = np.linalg.norm(v2)
        
        if norm_v1 < 1e-6 or norm_v2 < 1e-6:
            return 0.0
            
        cosine_angle = np.dot(v1, v2) / (norm_v1 * norm_v2)
        angle = np.arccos(np.clip(cosine_angle, -1.0, 1.0))
        return np.degrees(angle)

    # >> DTW 거리 계산 (Step 3)
    def _calculate_dtw(self, user_seq, expert_seq):
        # fastdtw 사용 (추천)
        # radius 매개변수를 추가하여 탐색 범위를 제한한다.
        # 30프레임(약 1초) 이상 차이나는 프레임은 매칭하지 않도록 강제한다.
        distance, path = fastdtw(user_seq, expert_seq, radius=30, dist=euclidean)
        return distance, path

    # >> 모양 점수 변환 (Shape Score)
    def _convert_distance_to_score(self, total_distance, path_length):
        if path_length == 0: return 0
        
        avg_distance = total_distance / path_length
        
        # Alpha 값 튜닝: 평균 오차가 10도일 때 약 80점
        alpha = 45.0 
        
        score = 100 * np.exp(-avg_distance / alpha)
        return max(0, min(100, score))

    # >> 개선사항1: 타이밍 점수 계산 (Timing Score)
    # >> DTW 경로가 대각선(y=x)에서 얼마나 벗어났는지를(Temporal Cost) 계산
    def _calculate_timing_score(self, path):
        if not path: return 0
        
        temporal_cost = 0
        for u_idx, e_idx in path:
            # 시간 차이(프레임 차이)의 절대값 누적
            temporal_cost += abs(u_idx - e_idx)
            
        avg_temporal_cost = temporal_cost / len(path)
        
        # Beta 값 튜닝: 평균 1초(30프레임) 밀리면 약 36점, 0.5초(15프레임) 밀리면 약 60점
        # 100 * exp(-30 / beta) = 36  => beta ~= 30
        beta = 30.0
        
        score = 100 * np.exp(-avg_temporal_cost / beta)
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
            # 주의: u_vec에는 전역 각도가 추가되어 길이가 9지만,
            # angle_names는 기존 8개이므로 인덱싱 에러 없이 앞부분만 매핑된다.
            diffs = np.abs(u_vec - e_vec)
            mean_diff = np.mean(diffs) # 전체적인 모양 오차 평균
            
            # 부위별로 오차 합산 (Torso Angle은 특정 부위에 할당하지 않음)
            for part_name, angles_in_part in self.BODY_PARTS.items():
                for angle_name in angles_in_part:
                    idx = angle_names.index(angle_name)
                    part_errors[part_name] += diffs[idx]
            
            # 프레임별 점수 기록 (프레임 단위 점수는 모양 정확도 위주로 표출)
            if u_idx < len(raw_frames):
                frame_score = self._convert_distance_to_score(mean_diff, 1)
                frame_scores[u_idx] = float(f"{frame_score:.1f}")

                frame_info = raw_frames[u_idx]
                
                # 타임라인 생성 (1초 단위)
                if frame_info['frame_index'] % 30 == 0:
                    start_sec = frame_info['timestamp']
                    end_sec = start_sec + 1.0
                    
                    timeline.append({
                        "start_sec": float(f"{start_sec:.2f}"),
                        "end_sec": float(f"{end_sec:.2f}"),
                        "score": int(frame_score),
                        "comment": "자세 정확도가 떨어집니다.",
                        "error_part_index": [i for i, d in enumerate(diffs[:8]) if d > 15.0] # 15도 이상 차이나는 인덱스 (전역각도 제외)
                    })

        # 가장 많이 틀린 부위 찾기
        worst_part = max(part_errors, key=part_errors.get)
        best_part = min(part_errors, key=part_errors.get)
        
        return worst_part, best_part, timeline, frame_scores
