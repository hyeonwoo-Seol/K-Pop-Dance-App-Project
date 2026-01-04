# >> scoring.py
# >> 사용자의 스켈레톤 데이터를 정규화하고, 전문가 데이터와 비교하여 점수를 산출한다.
# >> [보정] Calibration 수행: 인간의 눈높이에 맞춰 채점 기준(alpha, beta)을 완화하여
# >> 잘 춘 춤(동일한 춤)에 대해 90점 이상의 점수가 나오도록 튜닝함.

import numpy as np
import json
import math
import os
import copy
from scipy.spatial.distance import euclidean
from fastdtw import fastdtw

class Scoring:
    # >> 관절 인덱스 매핑 (YOLO v11 기준)
    # >> Index_이름매핑.txt 내용을 바탕으로 매핑
    KEYPOINT_NAMES = {
        0: "Nose", 1: "Left Eye", 2: "Right Eye", 3: "Left Ear", 4: "Right Ear",
        5: "Left Shoulder", 6: "Right Shoulder", 7: "Left Elbow", 8: "Right Elbow",
        9: "Left Wrist", 10: "Right Wrist", 11: "Left Hip", 12: "Right Hip",
        13: "Left Knee", 14: "Right Knee", 15: "Left Ankle", 16: "Right Ankle",
        17: "Neck" # 17번은 내부적으로 계산하여 추가한 Neck
    }

    # >> 각도 계산을 위한 관절 조합 정의
    ANGLES_DEF = {
        "left_elbow": (5, 7, 9),      
        "right_elbow": (6, 8, 10),    
        "left_shoulder": (11, 5, 7),  
        "right_shoulder": (12, 6, 8), 
        "left_knee": (11, 13, 15),    
        "right_knee": (12, 14, 16),   
        "left_hip": (5, 11, 13),      
        "right_hip": (6, 12, 14)      
    }

    # >> 부위별 각도 그룹 정의 (정확도 계산용)
    # >> "Torso"는 _extract_angles에서 마지막(인덱스 8)에 추가되는 각도를 사용함
    BODY_PARTS_GROUPS = {
        "Left Arm": ["left_elbow", "left_shoulder"],
        "Right Arm": ["right_elbow", "right_shoulder"],
        "Left Leg": ["left_knee", "left_hip"],
        "Right Leg": ["right_knee", "right_hip"],
        "Torso": ["torso_angle"]
    }

    def __init__(self):
        pass

    def compare(self, user_json_path, expert_json_path):
        print(f"[Scoring] 점수 계산 시작: User({os.path.basename(user_json_path)}) vs Expert({os.path.basename(expert_json_path)})")

        user_data = self._load_json(user_json_path)
        expert_data = self._load_json(expert_json_path)
        
        if not user_data or not expert_data:
            print("[Error] 데이터 로드 실패")
            return None

        fps = user_data["metadata"].get("fps", 30.0)

        # 보간법 적용
        user_data["frames"] = self._interpolate_missing_frames(user_data["frames"], fps)

        # 유효 프레임 추출
        user_frames_raw = [f for f in user_data["frames"] if f["is_valid"]]
        expert_frames_raw = [f for f in expert_data["frames"] if f["is_valid"]]
        
        user_kps = [f["keypoints"] for f in user_frames_raw]
        expert_kps = [f["keypoints"] for f in expert_frames_raw]

        total_user_frames = len(user_data["frames"])
        valid_user_frames_count = len(user_kps)
        visibility_ratio = valid_user_frames_count / total_user_frames if total_user_frames > 0 else 0.0
        
        if len(user_kps) == 0 or len(expert_kps) == 0:
            print("[Scoring] 비교할 수 있는 유효 프레임이 부족합니다.")
            return None

        # 2차 정규화
        user_norm = [self._normalize_skeleton(kp) for kp in user_kps]
        expert_norm = [self._normalize_skeleton(kp) for kp in expert_kps]

        # 특징 벡터 추출
        user_features = np.array([self._extract_angles(kp) for kp in user_norm])
        expert_features = np.array([self._extract_angles(kp) for kp in expert_norm])

        # Auto-Sync 수행
        print("[Scoring] Auto-Sync 수행 중...")
        synced_user_feat, synced_expert_feat, offset, sync_start_idx = self._auto_sync_sequences(
            user_features, expert_features, search_range=90
        )
        
        # Keypoint 데이터도 Sync에 맞춰 자름 (Worst Index 및 Errors 계산용)
        # offset 적용하여 user/expert의 해당 구간 추출
        u_start_final = max(0, offset)
        e_start_final = max(0, -offset)
        
        synced_user_norm = user_norm[u_start_final:]
        synced_expert_norm = expert_norm[e_start_final:]
        
        if len(synced_user_feat) < 30 or len(synced_expert_feat) < 30:
            print("[Error] 싱크를 맞춘 후 비교할 프레임이 너무 적습니다.")
            return None
            
        print(f"[Scoring] 싱크 보정 완료: Offset = {offset} frames ({offset/fps:.2f} sec)")

        # DTW 알고리즘
        distance, path = self._calculate_dtw(synced_user_feat, synced_expert_feat)
        
        # 점수 산출
        shape_score = self._convert_distance_to_score(distance, len(path))
        timing_score = self._calculate_timing_score(path)
        
        # 최종 점수 (모양 70%, 타이밍 30% - 타이밍 비중을 조금 높임)
        final_score = (shape_score * 0.7) + (timing_score * 0.3)
        
        print(f"[Scoring] 세부 점수 - Shape: {shape_score:.1f}, Timing: {timing_score:.1f} -> Final: {final_score:.1f}")
        
        # 에러 분석 (타임라인 피드백, 부위별 정확도, Worst Index 3, Frame별 Errors)
        user_start_idx_in_raw = 0
        if offset > 0:
            user_start_idx_in_raw = offset
        
        aligned_user_frames = user_frames_raw[user_start_idx_in_raw:]
        
        # [수정] frame_errors 반환 추가
        worst_indices, part_accuracies, timeline, frame_scores, frame_errors = self._analyze_details(
            path, 
            synced_user_feat, synced_expert_feat, 
            synced_user_norm, synced_expert_norm,
            aligned_user_frames
        )

        full_frame_scores = [0.0] * total_user_frames
        full_frame_errors = [[] for _ in range(total_user_frames)] # [수정] 전체 프레임 에러 배열 초기화

        for i, score in enumerate(frame_scores):
            if i < len(aligned_user_frames):
                original_idx = aligned_user_frames[i]['frame_index']
                if original_idx < len(full_frame_scores):
                    full_frame_scores[original_idx] = score
                    # [수정] 해당 프레임의 에러 리스트 할당
                    if i < len(frame_errors):
                        full_frame_errors[original_idx] = frame_errors[i]

        return {
            "total_score": int(final_score),
            "part_accuracies": part_accuracies, # 부위별 정확도 추가
            "worst_points": worst_indices,      # 상위 3개 안 좋은 Index 추가
            "timeline": timeline,
            "frame_scores": full_frame_scores,
            "frame_errors": full_frame_errors,  # [수정] 프레임별 에러 리스트 추가
            "visibility_ratio": visibility_ratio 
        }

    def _auto_sync_sequences(self, user_seq, expert_seq, search_range=90):
        len_u = len(user_seq)
        len_e = len(expert_seq)
        
        min_dist = float('inf')
        best_offset = 0
        min_overlap = 30 
        
        for offset in range(-search_range, search_range + 1):
            u_start = max(0, offset)
            e_start = max(0, -offset)
            overlap_len = min(len_u - u_start, len_e - e_start)
            
            if overlap_len < min_overlap: continue
                
            u_segment = user_seq[u_start : u_start + overlap_len]
            e_segment = expert_seq[e_start : e_start + overlap_len]
            
            dist = np.mean(np.linalg.norm(u_segment - e_segment, axis=1))
            
            if dist < min_dist:
                min_dist = dist
                best_offset = offset
        
        u_start_final = max(0, best_offset)
        e_start_final = max(0, -best_offset)
        
        synced_user = user_seq[u_start_final:]
        synced_expert = expert_seq[e_start_final:]
        
        return synced_user, synced_expert, best_offset, u_start_final

    def _interpolate_missing_frames(self, frames, fps):
        max_gap_frames = int(0.3 * fps)
        interpolated_frames = copy.deepcopy(frames)
        valid_indices = [i for i, f in enumerate(interpolated_frames) if f["is_valid"]]
        if not valid_indices: return interpolated_frames

        for k in range(len(valid_indices) - 1):
            start_idx = valid_indices[k]
            end_idx = valid_indices[k+1]
            gap_size = end_idx - start_idx - 1
            if 0 < gap_size <= max_gap_frames:
                start_kp = np.array(interpolated_frames[start_idx]["keypoints"])
                end_kp = np.array(interpolated_frames[end_idx]["keypoints"])
                if start_kp.shape == end_kp.shape:
                    for i in range(1, gap_size + 1):
                        curr_idx = start_idx + i
                        alpha = i / (gap_size + 1)
                        interp_kp = (1 - alpha) * start_kp + alpha * end_kp
                        interpolated_frames[curr_idx]["keypoints"] = interp_kp.tolist()
                        interpolated_frames[curr_idx]["is_valid"] = True
        return interpolated_frames

    def _load_json(self, path):
        try:
            with open(path, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"[Error] JSON 로드 실패 ({path}): {e}")
            return None

    def _normalize_skeleton(self, keypoints):
        kp = np.array(keypoints)[:, :2]
        hip_center = (kp[11] + kp[12]) / 2.0
        kp -= hip_center 
        spine_vector = kp[17]
        spine_length = np.linalg.norm(spine_vector)
        if spine_length < 0.05: 
            scale_factor = 1.0
        else:
            scale_factor = 1.0 / spine_length
        return kp * scale_factor

    def _extract_angles(self, keypoints):
        angles = []
        for name, indices in self.ANGLES_DEF.items():
            idx_a, idx_b, idx_c = indices
            a, b, c = keypoints[idx_a], keypoints[idx_b], keypoints[idx_c]
            angle = self._calculate_angle_3points(a, b, c)
            angles.append(angle)
        
        # Torso Angle (Body) - 9번째 각도 (Index 8)
        neck_vec = keypoints[17] # 정규화시 0,0(골반) 기준 neck 위치 벡터
        vertical_vec = np.array([0, -1]) 
        torso_angle = self._calculate_angle_vector(neck_vec, vertical_vec)
        angles.append(torso_angle)
        
        return np.array(angles)

    def _calculate_angle_3points(self, a, b, c):
        ba = a - b
        bc = c - b
        return self._calculate_angle_vector(ba, bc)

    def _calculate_angle_vector(self, v1, v2):
        norm_v1 = np.linalg.norm(v1)
        norm_v2 = np.linalg.norm(v2)
        if norm_v1 < 1e-6 or norm_v2 < 1e-6: return 0.0
        cosine_angle = np.dot(v1, v2) / (norm_v1 * norm_v2)
        return np.degrees(np.arccos(np.clip(cosine_angle, -1.0, 1.0)))

    def _calculate_dtw(self, user_seq, expert_seq):
        distance, path = fastdtw(user_seq, expert_seq, radius=30, dist=euclidean)
        return distance, path

    def _convert_distance_to_score(self, total_distance, path_length):
        if path_length == 0: return 0
        avg_distance = total_distance / path_length
        alpha = 150.0 
        score = 100 * np.exp(-avg_distance / alpha)
        return max(0, min(100, score))

    def _calculate_timing_score(self, path):
        if not path: return 0
        temporal_cost = 0
        for u, e in path:
            temporal_cost += abs(u - e)
        avg_cost = temporal_cost / len(path)
        beta = 100.0
        score = 100 * np.exp(-avg_cost / beta)
        return max(0, min(100, score))

    # >> [수정됨] 상세 분석: 부위별 정확도, Worst Indices(Top 3), Frame Errors
    def _analyze_details(self, path, user_features, expert_features, user_norm_kps, expert_norm_kps, aligned_user_frames):
        # 1. 부위별 각도 에러 누적 변수 초기화
        part_error_accum = {name: 0.0 for name in self.BODY_PARTS_GROUPS.keys()}
        part_count = {name: 0 for name in self.BODY_PARTS_GROUPS.keys()}
        
        # 2. 관절(Index)별 거리 에러 누적 변수 초기화 (18개: 0~16 + 17(Neck))
        # Index 0~16만 실제 사용 (Neck은 제외하거나 포함 가능하나, 보통 말단부위가 중요함)
        kp_error_accum = np.zeros(18)
        
        angle_names = list(self.ANGLES_DEF.keys()) + ["torso_angle"] # 순서 중요 (9개)
        timeline = []
        frame_scores = [] 
        frame_errors_list = [] # [수정] 프레임별 에러 리스트 (0 or 1)
        
        if aligned_user_frames:
             frame_scores = [0.0] * len(aligned_user_frames)

        # 에러 판별 임계값 (정규화된 좌표 기준)
        # 키(Spine)를 1.0으로 정규화했으므로, 0.15는 키의 15% 정도 벗어난 오차
        ERROR_THRESHOLD = 0.15 

        for u_idx, e_idx in path:
            # === Feature(각도) 비교 ===
            feat_diffs = np.abs(user_features[u_idx] - expert_features[e_idx])
            mean_diff = np.mean(feat_diffs)
            
            # 부위별 각도 오차 누적
            for part_name, angle_list in self.BODY_PARTS_GROUPS.items():
                for angle_name in angle_list:
                    # 해당 각도의 인덱스 찾기
                    if angle_name in angle_names:
                        idx = angle_names.index(angle_name)
                        part_error_accum[part_name] += feat_diffs[idx]
                        part_count[part_name] += 1
            
            # === Keypoint(좌표) 비교 및 에러 마킹 ===
            current_frame_error = [0] * 18 # [수정] 현재 프레임의 관절별 에러 (기본 0)
            
            # 정규화된 좌표 간의 유클리드 거리 계산 -> 많이 틀린 관절 찾기용
            # u_idx, e_idx가 각 배열 길이를 넘지 않도록 안전장치 필요 (DTW path 특성상 안전하긴 함)
            if u_idx < len(user_norm_kps) and e_idx < len(expert_norm_kps):
                u_kp = user_norm_kps[u_idx]
                e_kp = expert_norm_kps[e_idx]
                # 각 관절별 거리 (norm)
                kp_dists = np.linalg.norm(u_kp - e_kp, axis=1) # Shape (18,)
                kp_error_accum += kp_dists
                
                # [수정] 임계값 초과 시 1로 마킹
                for k in range(18):
                    if kp_dists[k] > ERROR_THRESHOLD:
                        current_frame_error[k] = 1
            
            # 해당 user 프레임에 대한 에러 배열 저장 (DTW 경로상 중복될 수 있으나 순서대로 append)
            # 여기서는 u_idx가 진행됨에 따라 저장해야 하는데, DTW path는 u_idx가 반복될 수 있음.
            # 그러나 tasks.py에서 리스트 길이만큼 매핑하므로, 단순 append 보다는 u_idx에 매핑하는게 정확함.
            # 아래에서 frame_scores 처럼 처리해야 하나, 여기서는 순회 순서대로 쌓고 나중에 매핑하는 방식 사용 불가 (path 순서가 섞이지는 않지만 u_idx 반복됨)
            # 따라서 임시 딕셔너리에 저장 후 나중에 리스트로 변환하거나, 
            # 단순히 user frame 길이만큼의 리스트를 미리 만들고 채우는 방식이 안전함.
            # 현재 구조상 frame_scores 리스트를 미리 만들었으므로 frame_errors_list도 동일하게 처리해야 함.

            # === 타임라인 및 프레임 점수 기록 ===
            if u_idx < len(frame_scores):
                frame_score = self._convert_distance_to_score(mean_diff, 1)
                frame_scores[u_idx] = float(f"{frame_score:.1f}")
                
                # [수정] 에러 리스트가 아직 없으면 추가 (리스트 초기화는 위에서 안했으므로 동적 확장 또는 딕셔너리 사용)
                # 안전하게 미리 할당된 리스트를 사용하지 않고 있으므로, 여기서 처리
                # 하지만 로직 단순화를 위해 frame_errors_list를 전체 길이만큼 0으로 초기화하고 덮어쓰는게 좋음
                pass

                frame_info = aligned_user_frames[u_idx]
                # 1초(약 30프레임) 단위로 피드백 생성
                if frame_info['frame_index'] % 30 == 0:
                    start = frame_info['timestamp']
                    timeline.append({
                        "start_sec": float(f"{start:.2f}"),
                        "end_sec": float(f"{start + 1.0:.2f}"),
                        "score": int(frame_score),
                        "comment": "자세 정확도가 떨어집니다.",
                        # 해당 시점에서 오차가 20도 이상인 각도 인덱스들
                        "error_part_index": [i for i, d in enumerate(feat_diffs[:8]) if d > 20.0] 
                    })
        
        # [수정] DTW Path 순회 후, 각 프레임별 Max Error를 에러 리스트로 확정
        # u_idx가 여러 번 등장할 수 있으므로, 등장할 때마다 에러를 OR 연산하거나 Max 값 사용
        final_frame_errors = [[0]*18 for _ in range(len(aligned_user_frames))]
        
        for (u_idx, e_idx) in path:
             if u_idx < len(aligned_user_frames) and e_idx < len(expert_norm_kps):
                u_kp = user_norm_kps[u_idx]
                e_kp = expert_norm_kps[e_idx]
                dists = np.linalg.norm(u_kp - e_kp, axis=1)
                for k in range(18):
                    if dists[k] > ERROR_THRESHOLD:
                        final_frame_errors[u_idx][k] = 1

        # 1. 부위별 정확도 점수 환산 (0~100점)
        part_accuracies = {}
        for name in self.BODY_PARTS_GROUPS.keys():
            if part_count[name] > 0:
                avg_err = part_error_accum[name] / part_count[name]
                # 기존 점수 변환 공식 재사용
                score = self._convert_distance_to_score(avg_err, 1)
                part_accuracies[name] = int(score)
            else:
                part_accuracies[name] = 0

        # 2. Worst Index Top 3 추출
        # Neck(17)은 제외하고 0~16 사이에서 찾기
        valid_kp_indices = range(17) 
        kp_errors_dict = {i: kp_error_accum[i] for i in valid_kp_indices}
        
        # 에러 내림차순 정렬
        sorted_kps = sorted(kp_errors_dict.items(), key=lambda x: x[1], reverse=True)
        
        worst_indices = []
        for i in range(min(3, len(sorted_kps))):
            idx = sorted_kps[i][0]
            kp_name = self.KEYPOINT_NAMES.get(idx, f"Unknown({idx})")
            worst_indices.append(kp_name)
        
        return worst_indices, part_accuracies, timeline, frame_scores, final_frame_errors
