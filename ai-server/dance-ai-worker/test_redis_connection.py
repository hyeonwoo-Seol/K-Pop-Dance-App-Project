import redis
import sys

# >> 로컬 Redis 서버에 접속하여 쓰기/읽기 테스트를 수행한다.
def test_redis_connection():
    print("🔄 Redis 서버 연결 시도 중...", end=" ")
    
    try:
        # Redis 클라이언트 생성 (기본 포트 6379, DB 0번)
        # 만약 Redis 비밀번호를 설정했다면 password='yourpassword' 추가 필요
        r = redis.Redis(host='localhost', port=6379, db=0)
        
        # 1. Ping 테스트
        if not r.ping():
            print("\n❌ 실패: Redis 서버가 응답하지 않습니다.")
            return False

        print("✅ 연결 성공!")

        # >> 데이터 쓰기 테스트
        test_key = "graduation_project_test"
        test_value = "Dance AI System OK"
        print(f"🔄 데이터 쓰기 테스트 (Key: {test_key})...", end=" ")
        r.set(test_key, test_value)
        print("완료")

        # >> 데이터 읽기 테스트
        print(f"🔄 데이터 읽기 테스트...", end=" ")
        retrieved_value = r.get(test_key)
        
        if retrieved_value:
            decoded_value = retrieved_value.decode('utf-8')
            print(f"완료 (값: {decoded_value})")
            
            if decoded_value == test_value:
                print("\n🎉 [성공] Redis 인프라가 정상적으로 구축되었습니다!")
                print("   이제 Celery 워커를 설정할 준비가 되었습니다.")
                
                # 테스트 데이터 정리
                r.delete(test_key)
                return True
            else:
                print("\n⚠️ 경고: 저장된 값과 읽어온 값이 다릅니다.")
                return False
        else:
            print("\n❌ 실패: 데이터를 읽어오지 못했습니다.")
            return False

    except redis.ConnectionError:
        print("\n\n❌ [치명적 오류] Redis 서버에 접속할 수 없습니다.")
        print("   1. Ubuntu 터미널에서 'sudo service redis-server start'를 실행했는지 확인하세요.")
        print("   2. 'redis-cli ping' 명령어가 PONG을 반환하는지 확인하세요.")
        return False
    except Exception as e:
        print(f"\n\n❌ 예상치 못한 오류 발생: {e}")
        return False

if __name__ == "__main__":
    success = test_redis_connection()
    if not success:
        sys.exit(1)
