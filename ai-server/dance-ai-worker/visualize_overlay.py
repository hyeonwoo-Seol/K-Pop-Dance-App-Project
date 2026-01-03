# >> visualize_overlay.py
# >> Source 영상과 User 영상에 각각 자신의 스켈레톤 데이터를 오버레이하여
# >> 2개의 독립적인 검증 영상(overlay_source.mp4, overlay_user.mp4)을 생성합니다.

import cv2
import json
import os
import numpy as np

def run_overlay_visualization():
    # 1. 파일 경로 설정
    base_dir = os.path.dirname(os.path.abspath(__file__))
    sample_dir = os.path.join(base_dir, 'sampleMP4')
    result_dir = os.path.join(base_dir, 'calibration_result')
    
    # 영상 파일 경로
    source_video_path = os.path.join(sample_dir, 'Cut_AfterLike_source.mp4')
    user_video_path = os.path.join(sample_dir, 'Cut_AfterLike_user.mp4')
    
    # JSON 파일 경로 찾기
    # 1) Source Expert JSON
    source_json_path = os.path.join(result_dir, "source_expert.json")
    
    # 2) User JSON (가장 최신 파일)
    user_json_files = [f for f in os.listdir(result_dir) if "user" in f and f.endswith(".json") and "ID" in f]
    if not user_json_files:
        print("[Error] User JSON 파일을 찾을 수 없습니다.")
        return
    user_json_name = max(user_json_files, key=lambda f: os.path.getmtime(os.path.join(result_dir, f)))
    user_json_path = os.path.join(result_dir, user_json_name)

    # 출력 파일 경로
    output_source = os.path.join(result_dir, "overlay_source_check.mp4")
    output_user = os.path.join(result_dir, "overlay_user_check.mp4")

    print(f"--- [ 개별 시각화 검증 시작 ] ---")
    
    # 2. 영상 생성 실행
    # (1) Source 영상 생성 (파랑/노랑 테마)
    if os.path.exists(source_video_path) and os.path.exists(source_json_path):
        print(f"\n[1/2] Source 영상 처리 중... ({os.path.basename(source_video_path)})")
        _create_single_overlay(
            source_video_path, 
            source_json_path, 
            output_source,
            bone_color=(0, 255, 255),  # Yellow
            joint_color=(255, 0, 0),   # Blue
            label="Expert (Source)"
        )
    else:
        print(f"[Skip] Source 영상 또는 JSON이 없습니다.")

    # (2) User 영상 생성 (초록/빨강 테마)
    if os.path.exists(user_video_path) and os.path.exists(user_json_path):
        print(f"\n[2/2] User 영상 처리 중... ({os.path.basename(user_video_path)})")
        _create_single_overlay(
            user_video_path, 
            user_json_path, 
            output_user,
            bone_color=(0, 255, 0),    # Green
            joint_color=(0, 0, 255),   # Red
            label="User (You)"
        )
    else:
        print(f"[Skip] User 영상 또는 JSON이 없습니다.")

    print(f"\n--- [ 모든 작업 완료 ] ---")
    print(f"1. {output_source}")
    print(f"2. {output_user}")
    print("👉 각 영상을 확인하여 뼈대가 사람 몸에 정확히 붙어있는지 확인하세요.")

def _create_single_overlay(video_path, json_path, output_path, bone_color, joint_color, label):
    # 데이터 로드
    with open(json_path, 'r') as f:
        pose_data = json.load(f)
    
    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))

    # 관절 연결
    connections = [
        (0,1), (0,2), (1,3), (2,4), 
        (5,6), (5,7), (7,9), (6,8), (8,10), 
        (5,11), (6,12), (11,12), 
        (11,13), (13,15), (12,14), (14,16)
    ]

    frames_data = pose_data['frames']
    frame_idx = 0
    
    while True:
        ret, frame = cap.read()
        if not ret: break
        
        # Draw Skeleton
        if frame_idx < len(frames_data):
            f_data = frames_data[frame_idx]
            if f_data['is_valid']:
                # 정규화 기준 (pose_estimation.py 로직 역산)
                max_dim = max(width, height)
                
                points = {}
                # Joints
                for i, (nx, ny, conf) in enumerate(f_data['keypoints']):
                    if conf > 0.3:
                        px = int(nx * max_dim)
                        py = int(ny * max_dim)
                        # Clipping
                        px = max(0, min(width - 1, px))
                        py = max(0, min(height - 1, py))
                        
                        points[i] = (px, py)
                        cv2.circle(frame, (px, py), 4, joint_color, -1)
                
                # Bones
                for u, v in connections:
                    if u in points and v in points:
                        cv2.line(frame, points[u], points[v], bone_color, 2)
        
        # Label
        cv2.putText(frame, f"{label} | Frame: {frame_idx}", (20, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, bone_color, 2)
        
        out.write(frame)
        if frame_idx % 100 == 0:
            print(f"   >> {frame_idx}/{total_frames} frames...")
        frame_idx += 1

    cap.release()
    out.release()
    print(f"   ✅ 저장 완료: {output_path}")

if __name__ == "__main__":
    run_overlay_visualization()
