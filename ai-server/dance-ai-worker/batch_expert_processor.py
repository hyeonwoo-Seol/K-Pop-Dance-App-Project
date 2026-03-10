# >> batch_expert_processor.py
# >> data/expert_videos 폴더 내의 모든 전문가 영상을 순회하여 AI 분석을 수행합니다.
# >> 생성된 JSON 파일은 scoring.py 및 tasks.py 시스템과 완벽히 호환되도록 파일명을 정제하여 저장합니다.

import os
import glob
from pose_estimation import PoseEstimator
from config import Config

def process_expert_videos_batch():
    # 1. 대상 디렉터리 설정 (config.py의 설정값 사용)
    expert_dir = Config.EXPERT_DIR
    
    if not os.path.exists(expert_dir):
        print(f"[Error] 전문가 데이터 디렉터리가 존재하지 않습니다: {expert_dir}")
        os.makedirs(expert_dir, exist_ok=True)
        print(f"[Info] 디렉터리를 생성했습니다. 영상을 넣고 다시 실행하세요.")
        return

    # 2. 영상 파일 검색
    video_extensions = ['*.mp4', '*.mov', '*.avi', '*.webm', '*.mkv']
    video_files = []
    
    for ext in video_extensions:
        # [수정] 하위 디렉터리([517], [477] 등)까지 재귀적으로 탐색하도록 변경
        search_pattern = os.path.join(expert_dir, '**', ext)
        video_files.extend(glob.glob(search_pattern, recursive=True))
    
    # 중복 제거 및 이름순 정렬
    video_files = sorted(list(set(video_files)))
    
    if not video_files:
        print(f"[Info] '{expert_dir}' 경로에 처리할 영상 파일이 없습니다.")
        return

    total_files = len(video_files)
    print("=" * 60)
    print(f"🎬 전문가 안무 영상 JSON 일괄 변환기")
    print(f"📂 대상 폴더: {expert_dir}")
    print(f"🔢 발견된 원본 영상: {total_files}개")
    print("=" * 60)
    
    # 3. AI 모델 초기화 (메모리 적재 및 워밍업 1회 수행)
    print("\n[AI] YOLO 모델을 로드합니다...")
    try:
        estimator = PoseEstimator(model_path='yolo11l-pose.pt')
    except Exception as e:
        print(f"[Error] 모델 초기화 실패: {e}")
        return

    success_count = 0
    
    # 4. 순차적 영상 분석 및 JSON 생성
    # [수정] 팀 합의안에 따라 파일명은 정확히 3개의 구성 요소로만 이루어지도록 검증합니다.
    for idx, video_path in enumerate(video_files):
        video_name = os.path.splitext(os.path.basename(video_path))[0]
        
        # [수정] 파일명 검증: 언더스코어('_')를 기준으로 정확히 3개의 조각으로 나뉘는지 확인
        # 형식: songID_Artist_PartNumber (어느 항목에도 '_' 포함 불가)
        parts = video_name.split('_')
        if len(parts) != 3:
            print(f"\n[{idx+1}/{total_files}] ⚠️ 파일명 규격 불일치 (건너뜀): {video_name}.mp4")
            print("   -> 요구 형식: 'songID_Artist_PartNumber' (정확히 2개의 '_' 필요, 예: 12_IVE_1.mp4)")
            continue
        
        # 목표 JSON 파일명 (tasks.py 호환을 위해 _result 제외)
        # [핵심] 영상이 하위 디렉터리에 있더라도, JSON은 expert_dir 최상위에 저장됨
        final_json_path = os.path.join(expert_dir, f"{video_name}.json")
        
        # 이미 처리된 파일인지 확인 (재실행 시 시간 절약)
        if os.path.exists(final_json_path):
            print(f"[{idx+1}/{total_files}] ⏭️ 이미 변환됨 (건너뜀): {video_name}.json")
            continue
            
        print(f"\n[{idx+1}/{total_files}] 🔄 전문가 영상 분석 중: {video_name}.mp4")
        
        try:
            # pose_estimation.py 내부 로직상 가장 큰 객체(전문가)를 자동으로 타겟팅합니다.
            # 반환되는 파일명은 {video_name}_result.json 형태입니다.
            temp_result_path = estimator.process_video(video_path, expert_dir)
            
            # scoring.py 매칭 규칙에 맞게 파일 이름 정제 (Rename)
            if temp_result_path and os.path.exists(temp_result_path):
                os.rename(temp_result_path, final_json_path)
                print(f"   ✅ 변환 및 규격화 저장 완료: {os.path.basename(final_json_path)}")
                success_count += 1
            else:
                print(f"   ❌ 결과 파일 생성 실패: {temp_result_path}")
                
        except Exception as e:
            print(f"   ❌ 분석 중 치명적 오류 발생 ({video_name}): {e}")

    print("\n" + "=" * 60)
    print(f"🎉 모든 전문가 영상 일괄 변환 작업 완료!")
    print(f"📊 성공: {success_count} / 전체 시도: {total_files}")
    print("=" * 60)

if __name__ == "__main__":
    process_expert_videos_batch()
