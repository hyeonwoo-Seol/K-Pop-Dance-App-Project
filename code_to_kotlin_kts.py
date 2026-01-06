import os
import math

def collect_and_split_kotlin_files():
    # 출력 파일 이름 정의 (총 5개)
    output_files = [
        'compiled_kotlin_1.txt', 
        'compiled_kotlin_2.txt', 
        'compiled_kotlin_3.txt',
        'compiled_kotlin_4.txt',
        'compiled_kotlin_5.txt'
    ]
    
    # 현재 스크립트가 위치한 디렉토리 파악
    current_dir = os.path.dirname(os.path.abspath(__file__))
    
    kt_files = []   # .kt 파일 저장 리스트
    kts_files = []  # .kts 파일 저장 리스트

    print(f"검색 시작 위치: {current_dir}")

    # 1. 하위 디렉토리를 포함한 모든 .kt 및 .kts 파일 탐색
    for root, dirs, files in os.walk(current_dir):
        for file in files:
            full_path = os.path.join(root, file)
            if file.endswith('.kt'):
                kt_files.append(full_path)
            elif file.endswith('.kts'):
                kts_files.append(full_path)

    total_kt = len(kt_files)
    total_kts = len(kts_files)
    print(f"검색 결과: .kt 파일 {total_kt}개, .kts 파일 {total_kts}개 찾음 (총 {total_kt + total_kts}개).")

    if total_kt + total_kts == 0:
        print("파일을 찾지 못해 종료합니다.")
        return

    # 2. .kt 파일을 4개의 묶음으로 분할
    kt_chunks = []
    if total_kt > 0:
        # 4개 파일로 나누기 위한 크기 계산 (올림 처리)
        chunk_size = math.ceil(total_kt / 4)
        for i in range(0, total_kt, chunk_size):
            kt_chunks.append(kt_files[i:i + chunk_size])
    
    # 파일 개수가 적어서 4개 묶음이 안 나올 경우 빈 리스트로 채워 4개 맞춤
    while len(kt_chunks) < 4:
        kt_chunks.append([])
    
    # 만약 계산상 4개를 초과하는 경우(극히 드묾) 앞 4개만 사용
    kt_chunks = kt_chunks[:4]

    # 3. 최종 묶음 구성 (kt 4개 + kts 1개)
    # 마지막 5번째 묶음은 .kts 파일 전체를 할당
    final_chunks = kt_chunks + [kts_files]

    # 4. 각 묶음을 txt 파일로 저장
    global_counter = 1  # 전체 파일에 대한 고유 번호 (모든 파일 통틀어 연속됨)

    for i in range(5):
        out_filename = output_files[i]
        file_list = final_chunks[i]
        
        # 현재 처리 중인 파일 종류 식별 (로그용)
        is_kts_container = (i == 4)
        file_type_label = ".kts 스크립트" if is_kts_container else ".kt 소스"
        
        try:
            with open(out_filename, 'w', encoding='utf-8') as out_f:
                # 해당 묶음에 파일이 없는 경우
                if not file_list:
                    out_f.write(f"[{file_type_label} 컨테이너]\n")
                    out_f.write("이 파일에는 해당되는 소스 코드가 없습니다.\n")
                    print(f"{out_filename} 생성 완료 (비어 있음 - {file_type_label}).")
                    continue

                for file_path in file_list:
                    try:
                        # 소스 코드 파일 읽기
                        with open(file_path, 'r', encoding='utf-8') as source_f:
                            content = source_f.read()
                        
                        filename = os.path.basename(file_path)
                        abs_path = os.path.abspath(file_path)

                        # 포맷 작성
                        out_f.write(f"# {global_counter}번 코드\n")
                        out_f.write(f"{filename}\n")
                        out_f.write(f"{abs_path}\n")
                        out_f.write(f"{content}\n")
                        
                        # 파일 간 구분을 위한 구분선
                        out_f.write("\n" + ("-" * 30) + "\n\n")
                        
                        global_counter += 1
                        
                    except Exception as e:
                        print(f"파일 읽기 오류 ({file_path}): {e}")
                        out_f.write(f"# {global_counter}번 코드 (읽기 오류)\n")
                        out_f.write(f"{os.path.basename(file_path)}\n")
                        out_f.write(f"{file_path}\n")
                        out_f.write(f"Error reading file: {e}\n\n")
                        global_counter += 1

            print(f"{out_filename} 생성 완료 ({len(file_list)}개 파일 포함).")

        except Exception as e:
            print(f"{out_filename} 생성 중 치명적 오류 발생: {e}")

if __name__ == "__main__":
    collect_and_split_kotlin_files()