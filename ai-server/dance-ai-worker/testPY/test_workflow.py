# >> SQS 없이 파이썬 코드로 직접 작업을 시켜보는 테스트 파일이다.

from tasks import download_video_task, pose_estimation_task
import os
import shutil

TEST_VIDEO_FILENAME = "[576]IVE원영_AfterLike_h264.mp4"

# >> 저장한 테스트 영상이 data/raw_videos 폴더에 실제로 있는지 확인한다.
def check_video_exists():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    target_path = os.path.join(base_dir, 'data', 'raw_videos', TEST_VIDEO_FILENAME)
    
    if not os.path.exists(target_path):
        print(f"\n [오류]파일을 찾을 수 없습니다!")
        print(f" 경로 확인: {target_path}")
        print(f" 위 경로에 '{TEST_VIDEO_FILENAME}' 파일이 들어있는지 확인해주세요.")
        return None
    
    print(f" 테스트 영상 확인됨: {TEST_VIDEO_FILENAME}")
    return TEST_VIDEO_FILENAME


def run_test():
    print(" [TEST] 로컬 워크플로우 테스트 시작 (Target: IVE After Like)")

    # 1. 영상 확인
    video_key = check_video_exists()
    if not video_key:
        return

    try:
        # 2. Step 1: 다운로드 Task 호출 (Mock)
        # S3에서 다운로드하는 척하면서, 실제로는 로컬 파일을 찾아 리턴합니다.
        print(f"\n [STEP1] Download Task 호출 (Mock) -> {video_key}")
        
        # 가상의 버킷 이름 "kpop-bucket" (테스트용이라 아무거나 상관없음)
        local_path = download_video_task("kpop-bucket", video_key)
        print(f"다운로드(매핑) 성공: {local_path}")

        # 3. Step 2: 분석 Task 호출 (YOLO v11)
        print(f"\n [STEP2] Pose Estimation Task 호출 (AI 분석 시작)")
        print("분석 중입니다... (영상 길이에 따라 시간이 걸릴 수 있습니다)")
        
        final_result = pose_estimation_task(local_path)
        
        print("\n [TEST] 모든 단계 완료!")
        print("="*50)
        print(f" 결과 파일 경로: {final_result.get('result_json')}")
        print("="*50)
        
    except Exception as e:
        print(f"\n [TEST] 실패: {e}")

if __name__ == "__main__":
    run_test()
