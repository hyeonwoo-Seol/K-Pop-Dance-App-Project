# >> visualize_overlay.py
# >> AI가 분석한 JSON 파일의 좌표를 읽어 원본 영상 위에 스켈레톤을 시각화합니다.
# >> 사람 감지 실패 원인을 분석하기 위한 디버깅용 도구입니다.

import cv2
import json
import os
import numpy as np
from config import Config

# ==========================================
# [시각화 설정] 관절 연결 정보 및 색상
# ==========================================
# YOLO v11 Pose Keypoints Mapping
# 0:Nose, 1:L-Eye, 2:R-Eye, 3:L-Ear, 4:R-Ear, 
# 5:L-Shoulder, 6:R-Shoulder, 7:L-Elbow, 8:R-Elbow, 9:L-Wrist, 10:R-Wrist, 
# 11:L-Hip, 12:R-Hip, 13:L-Knee, 14:R-Knee, 15:L-Ankle, 16:R-Ankle, 17:Neck

SKELETON_CONNECTIONS = [
    (0, 1), (0, 2), (1, 3), (2, 4),       # 얼굴
    (5, 6), (5, 7), (7, 9),               # 왼팔
    (6, 8), (8, 10),                      # 오른팔
    (5, 11), (6, 12),                     # 몸통 (어깨-골반)
    (11, 12),                             # 골반
    (11, 13), (13, 15),                   # 왼쪽 다리
    (12, 14), (14, 16),                   # 오른쪽 다리
    (5, 17), (6, 17)                      # 목 연결 (커스텀 17번 활용)
]

COLOR_BONE = (0, 255, 0)   # 초록색 (뼈)
COLOR_JOINT = (0, 0, 255)  # 빨간색 (관절)

def draw_skeleton(frame, keypoints, width, height):
    """
    정규화된(0.0~1.0) 키포인트 데이터를 받아 프레임에 그립니다.
    keypoints: [[x, y, conf], ...] (총 18개)
    """
    # 1. 픽셀 좌표로 변환
    # [수정] pose_estimation.py에서 max(width, height)로 정규화했으므로,
    # 복원할 때도 max_dim을 곱해야 비율이 유지됨 (특히 9:16 영상 등에서 중요)
    max_dim = max(width, height)
    
    pixel_kps = []
    for kp in keypoints:
        # kp[0]: x비율, kp[1]: y비율, kp[2]: 신뢰도(Confidence)
        # 기존: px = int(kp[0] * width) -> 오버레이 밀림 발생 원인
        px = int(kp[0] * max_dim)
        py = int(kp[1] * max_dim)
        conf = kp[2]
        pixel_kps.append((px, py, conf))

    # 2. 뼈대 그리기 (선)
    for idx_a, idx_b in SKELETON_CONNECTIONS:
        # 인덱스가 범위 내에 있고, 둘 다 신뢰도가 0보다 클 때만 그림
        if idx_a < len(pixel_kps) and idx_b < len(pixel_kps):
            pt_a = pixel_kps[idx_a]
            pt_b = pixel_kps[idx_b]
            
            # 신뢰도가 너무 낮으면(0) 그리지 않음 (0.0은 데이터 없음 의미)
            if pt_a[2] > 0 and pt_b[2] > 0:
                cv2.line(frame, (pt_a[0], pt_a[1]), (pt_b[0], pt_b[1]), COLOR_BONE, 2)

    # 3. 관절 그리기 (점)
    for i, pt in enumerate(pixel_kps):
        if pt[2] > 0:
            cv2.circle(frame, (pt[0], pt[1]), 4, COLOR_JOINT, -1)
            # 디버깅용: 관절 번호 표시 (선택)
            # cv2.putText(frame, str(i), (pt[0], pt[1]), cv2.FONT_HERSHEY_SIMPLEX, 0.3, (255, 255, 255), 1)

def process_video_overlay(task_name, video_path, json_path, output_filename):
    print(f"\n[Overlay] {task_name} 처리 시작")
    print(f"  - 영상 경로: {video_path}")
    print(f"  - JSON 경로: {json_path}")

    # 1. 파일 존재 확인
    if not os.path.exists(video_path):
        print(f"[Error] 영상 파일을 찾을 수 없습니다: {video_path}")
        return
    if not os.path.exists(json_path):
        print(f"[Error] JSON 파일을 찾을 수 없습니다: {json_path}")
        return

    # 2. JSON 로드
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
            frames_data = data.get("frames", [])
            print(f"  - JSON 로드 성공: 총 {len(frames_data)} 프레임 데이터")
    except Exception as e:
        print(f"[Error] JSON 파싱 실패: {e}")
        return

    # 3. 비디오 설정
    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    # 4. 출력 비디오 설정
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_filename, fourcc, fps, (width, height))

    print(f"  - 영상 정보: {width}x{height}, {fps:.2f}fps, {total_frames}frames")
    print(f"  - 결과 저장: {output_filename} (처리 중...)")

    frame_idx = 0
    valid_frames_count = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        # 현재 프레임에 해당하는 JSON 데이터 찾기
        current_kps = None
        
        # frames_data가 순서대로라고 가정하고 인덱스 접근
        if frame_idx < len(frames_data):
            frame_info = frames_data[frame_idx]
            # is_valid가 True인 경우에만 그림
            if frame_info.get("is_valid", False):
                current_kps = frame_info.get("keypoints", [])
                valid_frames_count += 1
            else:
                # 데이터가 없는 프레임 표시
                cv2.putText(frame, "No Skeleton Data", (50, 50), 
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

        # 스켈레톤 그리기
        if current_kps:
            if len(current_kps) >= 17:
                draw_skeleton(frame, current_kps, width, height)
                cv2.putText(frame, f"Frame: {frame_idx} (Valid)", (50, 50), 
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)

        # 진행률 표시
        if frame_idx % 100 == 0:
            print(f"    Processing... {frame_idx}/{total_frames}")

        out.write(frame)
        frame_idx += 1

    cap.release()
    out.release()
    
    print(f"  -> [완료] {output_filename} 생성됨 (유효 프레임: {valid_frames_count}/{total_frames})")

def run_all_visualizations():
    # ==========================================
    # [설정] verify_server_local.py에서 사용한 경로 및 파일명 정보
    # ==========================================
    # 1. 전문가 데이터
    # 원본: sampleMP4/Cut_AfterLike_source.mp4
    # JSON: data/expert_videos/AfterLike_IVE_Chorus.json
    
    expert_video = "sampleMP4/Cut_AfterLike_source.mp4"
    expert_json = os.path.join(Config.EXPERT_DIR, "AfterLike_IVE_Chorus.json")
    expert_output = "overlay_expert.mp4"

    # 2. 사용자 데이터 (시뮬레이션)
    # verify_server_local.py가 복사한 경로: data/raw_videos/UserTest01_AfterLike_IVE_Chorus.mp4
    # 생성된 JSON: data/analyzed_json/UserTest01_AfterLike_IVE_Chorus_result.json
    
    user_video = os.path.join(Config.DOWNLOAD_DIR, "UserTest01_AfterLike_IVE_Chorus.mp4")
    user_json = os.path.join(Config.RESULT_DIR, "UserTest01_AfterLike_IVE_Chorus_result.json")
    user_output = "overlay_user.mp4"

    print("="*50)
    print("오버레이 영상 생성 작업을 시작합니다.")
    print("="*50)

    # 전문가 영상 처리
    process_video_overlay("Expert Video", expert_video, expert_json, expert_output)
    
    print("-" * 30)

    # 사용자 영상 처리
    process_video_overlay("User Video", user_video, user_json, user_output)

    print("="*50)
    print("모든 작업이 완료되었습니다.")
    print("="*50)

if __name__ == "__main__":
    run_all_visualizations()
