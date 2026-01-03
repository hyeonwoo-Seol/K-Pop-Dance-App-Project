# >> debug_analysis.py
# >> 현재 점수가 낮은 원인을 찾기 위해 3가지 변형(원본, 좌우반전, 회전)을 시도하고
# >> 스켈레톤을 시각화하여 영상으로 저장하는 진단 도구입니다.

import json
import cv2
import numpy as np
import os
import copy
from scoring import Scoring

def run_debug_analysis():
    # 1. 파일 경로 설정 (calibration_test.py 결과물 사용)
    base_dir = os.path.dirname(os.path.abspath(__file__))
    output_dir = os.path.join(base_dir, 'calibration_result')
    
    # [주의] calibration_test.py를 먼저 실행해서 아래 파일들이 생성되어 있어야 합니다.
    # 만약 파일명이 다르면 수정해주세요. (가장 최근 생성된 ID 파일 자동 탐색)
    source_path = os.path.join(output_dir, "source_expert.json")
    
    # user 파일 찾기 (ID가 붙은 파일 중 가장 최신 것)
    user_files = [f for f in os.listdir(output_dir) if "user" in f and f.endswith(".json") and "ID" in f]
    if not user_files:
        print("[Error] User JSON 파일을 찾을 수 없습니다. calibration_test.py를 먼저 실행하세요.")
        return
    user_path = os.path.join(output_dir, user_files[-1]) # 가장 나중 파일

    print(f"--- [ 진단 시작 ] ---")
    print(f"Source: {os.path.basename(source_path)}")
    print(f"User  : {os.path.basename(user_path)}")

    scorer = Scoring()
    
    # 2. 데이터 로드
    with open(source_path, 'r') as f: expert_data = json.load(f)
    with open(user_path, 'r') as f: user_data = json.load(f)

    # 3. 가설 검증 테스트
    print("\n🔍 [1단계] 데이터 변형 테스트 (점수가 오르는지 확인)")
    
    # Case 1: 원본 그대로
    score_original = _calculate_score(scorer, user_data, expert_data)
    print(f"1. 원본 그대로 비교: {score_original}점")

    # Case 2: 좌우 반전 (Mirror)
    user_mirror = copy.deepcopy(user_data)
    _apply_mirror(user_mirror)
    score_mirror = _calculate_score(scorer, user_mirror, expert_data)
    print(f"2. 좌우 반전(Mirror) 적용: {score_mirror}점")

    # Case 3: 시계방향 90도 회전 (Rotation) - 눕혀서 찍힌 경우
    user_rotate = copy.deepcopy(user_data)
    _apply_rotation(user_rotate)
    score_rotate = _calculate_score(scorer, user_rotate, expert_data)
    print(f"3. 90도 회전 적용: {score_rotate}점")

    # 4. 결론 도출
    best_score = max(score_original, score_mirror, score_rotate)
    if best_score == score_mirror:
        print("\n✅ [진단 결과] '좌우 반전' 문제입니다! 앱이나 서버에서 데이터를 반전시켜야 합니다.")
        final_user_data = user_mirror
        suffix = "_mirrored"
    elif best_score == score_rotate:
        print("\n✅ [진단 결과] '영상 회전' 문제입니다! 영상이 돌아가 있습니다.")
        final_user_data = user_rotate
        suffix = "_rotated"
    else:
        print("\n❓ [진단 결과] 반전/회전 문제가 아닙니다. 시각화 영상을 확인해보세요.")
        final_user_data = user_data
        suffix = "_original"

    # 5. 시각화 영상 생성
    print("\n🎥 [2단계] 스켈레톤 비교 영상 생성 중... (debug_comparison.mp4)")
    _create_comparison_video(expert_data, final_user_data, output_dir)

def _calculate_score(scorer, user_data, expert_data):
    # 임시 파일로 저장 후 scorer 호출 (scorer 구조상 파일 경로 필요)
    temp_u = "temp_user_debug.json"
    temp_e = "temp_expert_debug.json"
    with open(temp_u, 'w') as f: json.dump(user_data, f)
    with open(temp_e, 'w') as f: json.dump(expert_data, f)
    
    try:
        # scoring.py의 print문 억제
        import sys, io
        sys.stdout = io.StringIO()
        result = scorer.compare(temp_u, temp_e)
        sys.stdout = sys.__stdout__ # 복구
        
        if result: return result['total_score']
    except:
        sys.stdout = sys.__stdout__
    return 0

def _apply_mirror(json_data):
    # X 좌표 반전 (0.5 기준 대칭) 및 좌우 관절 ID 스왑
    # 1. 0.0 ~ 1.0 정규화 가정하에 x = 1.0 - x
    # 2. Left(odd) <-> Right(even) Swap
    
    # 관절 매핑 (YOLO 17 keypoints)
    # 1:L-Eye <-> 2:R-Eye ... 
    swap_pairs = [(1,2), (3,4), (5,6), (7,8), (9,10), (11,12), (13,14), (15,16)]
    
    for frame in json_data['frames']:
        if not frame['is_valid']: continue
        kp = frame['keypoints'] # List of [x, y, conf]
        
        # 좌표 반전
        for i in range(len(kp)):
            if kp[i][2] > 0: # conf > 0
                kp[i][0] = 1.0 - kp[i][0]
        
        # ID 스왑
        new_kp = copy.deepcopy(kp)
        for i, j in swap_pairs:
            new_kp[i] = kp[j]
            new_kp[j] = kp[i]
        frame['keypoints'] = new_kp

def _apply_rotation(json_data):
    # (x, y) -> (-y, x) 회전 변환 (90도)
    for frame in json_data['frames']:
        if not frame['is_valid']: continue
        for i in range(len(frame['keypoints'])):
            x, y, c = frame['keypoints'][i]
            # 중심(0.5, 0.5) 기준으로 회전
            # new_x = y
            # new_y = 1.0 - x
            frame['keypoints'][i][0] = y
            frame['keypoints'][i][1] = 1.0 - x

def _create_comparison_video(expert_data, user_data, output_dir):
    # 캔버스 설정
    W, H = 600, 600
    fps = 30
    
    # 프레임 수 맞추기
    len_e = len(expert_data['frames'])
    len_u = len(user_data['frames'])
    max_len = min(len_e, len_u) # 둘 중 짧은 쪽에 맞춤 (싱크 확인용)

    save_path = os.path.join(output_dir, "debug_comparison.mp4")
    out = cv2.VideoWriter(save_path, cv2.VideoWriter_fourcc(*'mp4v'), fps, (W*2, H))

    # 연결선 (YOLO 기준)
    connections = [
        (0,1), (0,2), (1,3), (2,4), # Face
        (5,6), (5,7), (7,9), (6,8), (8,10), # Arms
        (5,11), (6,12), (11,12), # Torso
        (11,13), (13,15), (12,14), (14,16) # Legs
    ]

    for i in range(max_len):
        canvas = np.zeros((H, W*2, 3), dtype=np.uint8)
        
        # Expert 그리기 (왼쪽)
        _draw_skeleton(canvas, expert_data['frames'][i], (0, 0), W, H, connections, (0, 255, 0))
        cv2.putText(canvas, "Expert (Source)", (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
        
        # User 그리기 (오른쪽)
        _draw_skeleton(canvas, user_data['frames'][i], (W, 0), W, H, connections, (0, 0, 255))
        cv2.putText(canvas, "User (You)", (W+50, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

        out.write(canvas)
    
    out.release()
    print(f"✅ 영상 저장 완료: {save_path}")
    print(f"👉 이 영상을 재생하면 왜 점수가 낮은지 바로 알 수 있습니다.")

def _draw_skeleton(img, frame_obj, offset, W, H, connections, color):
    if not frame_obj['is_valid']: return
    ox, oy = offset
    kps = frame_obj['keypoints']
    
    # 1. 정규화 해제 및 그리기
    # scoring.py의 2차 정규화(위치이동)가 적용되지 않은 '원본 좌표' 상태임.
    # 화면에 잘 보이게 스케일링
    
    points = {}
    for idx, (x, y, conf) in enumerate(kps):
        if conf > 0.3: # 신뢰도 체크
            px = int(x * W) + ox
            py = int(y * H) + oy
            points[idx] = (px, py)
            cv2.circle(img, (px, py), 4, color, -1)
            
    for a, b in connections:
        if a in points and b in points:
            cv2.line(img, points[a], points[b], color, 2)

if __name__ == "__main__":
    run_debug_analysis()
