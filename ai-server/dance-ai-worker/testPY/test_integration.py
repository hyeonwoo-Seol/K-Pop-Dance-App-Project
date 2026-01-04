import os
import time
from tasks import pose_estimation_task
from config import Config

def run_local_integration_test():
    # 1. 테스트 대상 영상 목록 가져오기
    video_dir = Config.DOWNLOAD_DIR  # data/raw_videos
    if not os.path.exists(video_dir):
        print(f"❌ [Error] 영상 폴더가 없습니다: {video_dir}")
        return

    # mp4 파일만 골라내기
    video_files = [f for f in os.listdir(video_dir) if f.endswith('.mp4')]
    
    if not video_files:
        print(f"❌ [Error] 테스트할 mp4 파일이 없습니다. {video_dir}를 확인하세요.")
        return

    print(f"🚀 [Test] 통합 테스트 시작! (총 {len(video_files)}개 영상)")
    print("=" * 60)

    success_count = 0
    fail_count = 0

    # 2. 각 영상에 대해 Celery 작업 요청 (동기적 실행으로 테스트)
    for idx, file_name in enumerate(video_files):
        video_path = os.path.join(video_dir, file_name)
        print(f"\n[{idx+1}/{len(video_files)}] 분석 요청: {file_name}")
        
        start_time = time.time()
        
        try:
            # Celery 워커가 켜져 있다면 .delay()를 써야 하지만,
            # 여기서는 로직 검증을 위해 직접 함수를 호출하여 결과를 바로 봅니다.
            # 실제 워커 환경 테스트를 원하시면 .delay()를 쓰고 로그를 확인해야 합니다.
            
            # [Case A] 함수 직접 호출 (디버깅용, 즉시 결과 확인)
            result = pose_estimation_task(video_path)
            
            # [Case B] Celery 워커에 요청 보내기 (실전 시뮬레이션)
            # task = pose_estimation_task.delay(video_path)
            # result = task.get(timeout=300) # 5분 대기
            
            elapsed = time.time() - start_time
            
            if result.get("status") == "success":
                print(f"   ✅ 성공! ({elapsed:.2f}초 소요)")
                print(f"   📂 결과: {result.get('result_path')}")
                success_count += 1
            else:
                print(f"   ❌ 실패: {result.get('error_message')}")
                fail_count += 1
                
        except Exception as e:
            print(f"   ❌ [Critical Error] 테스트 중단: {e}")
            fail_count += 1

    print("=" * 60)
    print(f"📊 테스트 요약: 성공 {success_count}건 / 실패 {fail_count}건")
    print("   결과 파일은 'data/analyzed_json' 폴더를 확인하세요.")

if __name__ == "__main__":
    # AWS 연결 없이 로컬 모드인지 확인
    if Config.USE_AWS:
        print("⚠️ 주의: USE_AWS=True로 설정되어 있습니다. 로컬 테스트를 위해 False로 간주하고 진행합니다.")
    
    run_local_integration_test()
