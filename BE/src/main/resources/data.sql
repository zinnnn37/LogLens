-- H2 Database 초기 테스트 데이터
-- dev 프로필에서만 사용됩니다

-- 테스트 사용자 생성
-- 비밀번호: test1234 (BCrypt 암호화)
INSERT INTO users (name, email, password, created_at, updated_at) VALUES
('테스트 사용자', 'test@test.com', '$2a$10$N5yVfEU.VmPJjHUyPfZo3.P4hUDKVXl3nz1HUmJvqvfyX5Nx.VVl2', NOW(), NOW());

-- 테스트 프로젝트 생성
INSERT INTO projects (project_name, description, project_uuid, created_at, updated_at) VALUES
('테스트 프로젝트', '로그 분석 테스트를 위한 프로젝트입니다.', '550e8400-e29b-41d4-a716-446655440000', NOW(), NOW());

-- 프로젝트 멤버 연결
INSERT INTO project_members (user_id, project_id, joined_at, created_at, updated_at) VALUES
(1, 1, NOW(), NOW(), NOW());
