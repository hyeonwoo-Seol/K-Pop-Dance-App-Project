import cv2
import json
import os
import numpy as np

# COCO Keypoint 연결 정보 (뼈대 그리기용)
SKELETON_CONNECTIONS = [
    (0, 1), (0, 2), (1, 3), (2, 4),           # 얼굴
    (5, 6), (5, 7), (7, 9),                   # 왼팔
    (6, 8), (8, 10),                          # 오른팔
    (5, 11), (6, 12),                         # 몸통
    (11, 12),                                 # 골반
    (11, 13), (13, 15),                       # 왼다리
    (12, 14), (14, 16)                        # 오른다리
]

# 색상 (BGR)
COLOR_POINT = (0, 255, 0)    # 초록색 (관절)
COLOR_LINE = (0, 255, 255)   # 노란색 (뼈대)

def visualize_json(video_path, json_path, output_path):
    # 1. 파일 로드
    if not os.path.exists(video_path):
        print(f"❌ 영상 파일 없음: {video_path}")
        return
    if not os.path.exists(json_path):
        print(f"❌ JSON 파일 없음: {json_path}")
        return

    with open(json_path, 'r') as f:
        data = json.load(f)

    cap = cv2.VideoCapture(video_path)
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = cap.get(cv2.CAP_PROP_FPS)
    
    # 2. 결과 영상 설정
    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    out = cv2.VideoWriter(output_path, fourcc, fps, (width, height))
    
    print(f"🎬 시각화 시작: {os.path.basename(video_path)}")
    print(f"   해상도: {width}x{height}, 총 프레임: {len(data['frames'])}")

    frame_idx = 0
    frames_data = data['frames']

    while True:
        ret, frame = cap.read()
        if not ret:
            break
        
        # 현재 프레임에 해당하는 JSON 데이터 가져오기
        if frame_idx < len(frames_data):
            frame_info = frames_data[frame_idx]
            
            if frame_info['is_valid']:
                # 정규화된 좌표를 다시 픽셀 좌표로 변환
                keypoints = frame_info['keypoints']
                pixel_points = []
                
                # 1. 모든 점 좌표 변환
                for kp in keypoints:
                    nx, ny, conf = kp
                    px, py = int(nx * width), int(ny * height)
                    pixel_points.append((px, py))
                    
                    # 신뢰도가 너무 낮으면 그리지 않음 (옵션)
                    if conf > 0.3:
                        cv2.circle(frame, (px, py), 5, COLOR_POINT, -1)

                # 2. 뼈대(Line) 그리기
                for idx1, idx2 in SKELETON_CONNECTIONS:
                    # 인덱스 범위 체크 (17개 점 기준)
                    if idx1 < len(pixel_points) and idx2 < len(pixel_points):
                        pt1 = pixel_points[idx1]
                        pt2 = pixel_points[idx2]
                        
                        # 두 점 모두 신뢰도가 있을 때만 그림 (여기선 좌표가 0이 아니면 그림)
                        if pt1 != (0,0) and pt2 != (0,0):
                            cv2.line(frame, pt1, pt2, COLOR_LINE, 2)

        # 3. 진행률 표시
        if frame_idx % 100 == 0:
            print(f"   Rendering... {frame_idx} frames")

        out.write(frame)
        frame_idx += 1

    cap.release()
    out.release()
    print(f"✅ 저장 완료: {output_path}")

if __name__ == "__main__":
    # 설정: 확인할 영상과 JSON 경로를 지정하세요.
    # 예시: 가장 먼저 테스트했던 파일 하나만 골라서 확인
    target_video_name = "[568]프나백지헌_StayThisWay_h264" # 확장자 제외 이름
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    video_file = os.path.join(base_dir, "data", "raw_videos", f"{target_video_name}.mp4")
    json_file = os.path.join(base_dir, "data", "analyzed_json", f"{target_video_name}_analysis.json")
    
    # 결과 저장 폴더 생성
    output_dir = os.path.join(base_dir, "data", "visualized_output")
    os.makedirs(output_dir, exist_ok=True)
    
    output_file = os.path.join(output_dir, f"{target_video_name}_overlay.mp4")

    visualize_json(video_file, json_file, output_file)
