#!/bin/bash

echo "🧪 MySQL 로그 테스트 시작"
echo "======================================"

# 1. 정상 쿼리 (General Log에 기록)
echo "1️⃣ 정상 SELECT 쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT * FROM users LIMIT 5;
"

sleep 1

# 2. 느린 쿼리 (Slow Query Log에 기록)
echo "2️⃣ 느린 쿼리 실행 (SLEEP 1초)..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT SLEEP(1);
"

sleep 1

# 3. 인덱스 없는 쿼리 (Slow Query Log에 기록)
echo "3️⃣ 인덱스 미사용 쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT * FROM users WHERE name LIKE '%test%';
"

sleep 1

# 4. 복잡한 쿼리 (Slow Query Log에 기록 가능)
echo "4️⃣ 복잡한 JOIN 쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT u.*, COUNT(*) as count
FROM users u
WHERE u.email LIKE '%@test.com'
GROUP BY u.id
HAVING count > 0;
"

sleep 1

# 5. 에러 발생 (Error Log에 기록)
echo "5️⃣ 에러 발생 - 존재하지 않는 테이블 조회..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT * FROM nonexistent_table;
" 2>&1 | grep -i error || echo "에러 발생됨"

sleep 1

# 6. 문법 오류 (Error Log에 기록)
echo "6️⃣ 에러 발생 - 잘못된 SQL 문법..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELCT * FROM users;
" 2>&1 | grep -i error || echo "에러 발생됨"

sleep 1

# 7. 권한 오류 (Error Log에 기록)
echo "7️⃣ 에러 발생 - 권한 없는 작업 시도..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
CREATE DATABASE unauthorized_db;
" 2>&1 | grep -i error || echo "에러 발생됨"

sleep 1

# 8. 대량 데이터 조회 (Slow Query Log에 기록)
echo "8️⃣ 대량 데이터 조회..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT u1.*, u2.*
FROM users u1
CROSS JOIN users u2
LIMIT 1000;
"

sleep 1

# 9. 서브쿼리 (느릴 가능성)
echo "9️⃣ 서브쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT * FROM users
WHERE id IN (SELECT id FROM users WHERE email LIKE '%test%');
"

sleep 1

# 10. UPDATE 쿼리
echo "🔟 UPDATE 쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
UPDATE users SET name = CONCAT(name, '_updated') WHERE id = 1;
"

sleep 1

# 11. DELETE 쿼리 (없는 데이터)
echo "1️⃣1️⃣ DELETE 쿼리 실행..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
DELETE FROM users WHERE id = 99999;
"

sleep 1

# 12. 트랜잭션 테스트
echo "1️⃣2️⃣ 트랜잭션 테스트..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
START TRANSACTION;
INSERT INTO users (name, email) VALUES ('tx_test', 'tx@test.com');
ROLLBACK;
"

sleep 1

# 13. 매우 느린 쿼리 (2초)
echo "1️⃣3️⃣ 매우 느린 쿼리 (2초 SLEEP)..."
docker exec test-mysql mysql -utestuser -ptest1234 testdb -e "
SELECT SLEEP(2);
"

echo ""
echo "======================================"
echo "✅ 테스트 완료!"
echo ""
echo "📊 로그 파일 크기 확인:"
ls -lh ../logs/infra/mysql/

echo ""
echo "📝 Error Log 마지막 10줄:"
tail -10 ../logs/infra/mysql/error.log

echo ""
echo "📝 Slow Query Log 마지막 20줄:"
tail -20 ../logs/infra/mysql/slow.log

