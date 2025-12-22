import subprocess
import os
import glob
import time

# ==========================================
# [설정] 전문가 안무 영상이 들어있는 폴더 경로
# 변환할 영상들을 이 폴더에 몰아넣고 실행하세요.
# 예: "data/expert_videos" 또는 절대 경로 사용 가능
TARGET_FOLDER = "data/expert_videos"

# 변환된 파일명 뒤에 붙을 접미사 (예: 영상.mp4 -> 영상_h264.mp4)
SUFFIX = "_h264"
# ==========================================

def convert_to_h264(input_path):
    """
    단일 파일을 H.264로 변환하는 함수 (GPU 가속 사용)
    """
    if not os.path.exists(input_path):
        print(f"❌ 파일 없음: {input_path}")
        return False

    # 파일명 분리 (경로, 이름, 확장자)
    dir_name, full_filename = os.path.split(input_path)
    filename, ext = os.path.splitext(full_filename)

    # 이미 변환된 파일이면 건너뛰기
    if filename.endswith(SUFFIX):
        print(f"⏭️  건너뜀 (이미 변환됨): {full_filename}")
        return True

    # 출력 파일명 생성
    output_filename = f"{filename}{SUFFIX}.mp4"
    output_path = os.path.join(dir_name, output_filename)

    print(f"🔥 변환 시작: {full_filename} --> {output_filename}")

    # FFmpeg 명령어 구성 (RTX 5060 Ti 가속 최적화)
    command_gpu = [
        'ffmpeg',
        '-i', input_path,         # 입력 파일
        '-c:v', 'h264_nvenc',     # NVIDIA GPU 인코딩
        '-preset', 'p4',          # 속도/화질 균형 프리셋
        '-b:v', '5M',             # 비트레이트 (5Mbps 정도면 모바일/태블릿에 충분히 고화질)
        '-c:a', 'aac',            # 오디오 코덱 (AAC가 호환성이 가장 좋음)
        '-b:a', '192k',           # 오디오 음질
        '-y',                     # 덮어쓰기 허용
        output_path
    ]

    # CPU 명령어 (GPU 실패 시 백업용)
    command_cpu = [
        'ffmpeg', '-i', input_path,
        '-c:v', 'libx264', '-crf', '23', '-preset', 'fast',
        '-c:a', 'aac', '-b:a', '192k',
        '-y', output_path
    ]

    start_time = time.time()
    try:
        # GPU 변환 시도
        subprocess.run(command_gpu, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True)
        duration = time.time() - start_time
        print(f"   ✅ [GPU 성공] 소요 시간: {duration:.2f}초")
        return True

    except subprocess.CalledProcessError:
        print("   ⚠️  GPU 변환 실패, CPU로 전환합니다...")
        try:
            # CPU 변환 시도
            start_time = time.time()
            subprocess.run(command_cpu, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True)
            duration = time.time() - start_time
            print(f"   ✅ [CPU 성공] 소요 시간: {duration:.2f}초")
            return True
        except subprocess.CalledProcessError as e:
            print(f"   ❌ [실패] 변환 불가: {e}")
            return False

def process_directory(target_dir):
    """
    폴더 내의 모든 영상 파일을 찾아 일괄 변환
    """
    # 1. 폴더 확인
    if not os.path.exists(target_dir):
        print(f"❌ 폴더를 찾을 수 없습니다: {target_dir}")
        print(f"📂 '{target_dir}' 폴더를 만들고 전문가 영상을 넣어주세요.")
        return

    # 2. 변환 대상 확장자 목록
    extensions = ['*.mp4', '*.mkv', '*.webm', '*.avi', '*.mov']
    video_files = []

    for ext in extensions:
        # 하위 폴더까지 검색하고 싶으면 recursive=True 옵션 사용
        video_files.extend(glob.glob(os.path.join(target_dir, ext)))

    # 중복 제거 및 정렬
    video_files = sorted(list(set(video_files)))
    total_files = len(video_files)

    print("="*60)
    print(f"🎬 전문가 안무 영상 일괄 변환기 (AV1 -> H.264)")
    print(f"📂 대상 폴더: {target_dir}")
    print(f"🔢 발견된 파일: {total_files}개")
    print("="*60)

    if total_files == 0:
        print("⚠️  변환할 영상 파일이 없습니다.")
        return

    # 3. 순차 변환
    success_count = 0
    for idx, video_path in enumerate(video_files):
        print(f"\n[{idx+1}/{total_files}] 처리 중...")
        if convert_to_h264(video_path):
            success_count += 1

    print("\n" + "="*60)
    print(f"🎉 모든 작업 완료!")
    print(f"📊 성공: {success_count} / 전체: {total_files}")
    print(f"📂 변환된 파일들은 '{target_dir}' 폴더에 '_h264'가 붙어서 저장되었습니다.")
    print("="*60)

if __name__ == "__main__":
    # 사용자가 경로를 직접 입력하지 않도록 상단 설정을 사용
    # 필요하면 여기에 절대 경로를 직접 적어도 됨
    
    # 예: 윈도우 경로인 경우 r"C:\Users\User\Videos\Kpop" 처럼 r을 붙여 사용
    base_dir = os.path.dirname(os.path.abspath(__file__))
    target_full_path = os.path.join(base_dir, TARGET_FOLDER)
    
    # 폴더가 없으면 생성 (안내용)
    if not os.path.exists(target_full_path):
        os.makedirs(target_full_path, exist_ok=True)
        print(f"📁 '{TARGET_FOLDER}' 폴더가 생성되었습니다. 여기에 전문가 영상을 넣고 다시 실행하세요.")
    else:
        process_directory(target_full_path)
