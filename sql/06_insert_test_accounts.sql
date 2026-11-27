-- ============================================
-- 演示测试账号：教师测试账号 + 学生测试账号
-- 库：course_ai（MySQL 8.0，utf8mb4）
-- 说明：
--   1. 两个测试账号密码统一为 123456，复用平台现有 BCrypt 哈希。
--   2. 学生测试账号加入全部班级，可见全部已发布教学任务及其关联课件。
--   3. 教师测试账号承接现有全部课件、班级、课程归属；教学任务随班级归属到该教师，
--      因此教师端“我的课件 / 我的任务 / 我的班级 / 我的课程”均为全量。
--   4. 脚本可重复执行（账号与选课记录均做了幂等判断）。
-- 执行方式：mysql -u root -p --default-character-set=utf8mb4 < 06_insert_test_accounts.sql
-- ============================================

USE course_ai;

-- 现有 BCrypt 哈希，对应明文密码 123456
SET @pwd_hash := '$2a$10$uYTQtSUFJpJ.0ykmfLToP.Jj6qZbzIYRZOHJN8inb2sKaOBx/wYiu';

-- 1. 新建教师测试账号（幂等：用户名已存在则跳过）
INSERT INTO sys_user (username, password, role, real_name, phone, email, created_at)
SELECT 'teacher_test', @pwd_hash, 'teacher', '测试教师', '13900000001', 'teacher_test@demo.edu.cn', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'teacher_test');

-- 2. 新建学生测试账号（幂等：用户名已存在则跳过）
INSERT INTO sys_user (username, password, role, real_name, phone, email, created_at)
SELECT 'student_test', @pwd_hash, 'student', '测试学生', '13900000002', 'student_test@demo.edu.cn', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'student_test');

-- 3. 教师测试账号承接现有全部动画课件归属
UPDATE animation
SET teacher_id = (SELECT id FROM sys_user WHERE username = 'teacher_test')
WHERE teacher_id <> (SELECT id FROM sys_user WHERE username = 'teacher_test');

-- 4. 承接全部班级归属（任务通过 class_id 关联班级，随之归属到该教师）
UPDATE class_info
SET teacher_id = (SELECT id FROM sys_user WHERE username = 'teacher_test')
WHERE teacher_id <> (SELECT id FROM sys_user WHERE username = 'teacher_test');

-- 5. 承接全部课程归属
UPDATE course
SET teacher_id = (SELECT id FROM sys_user WHERE username = 'teacher_test')
WHERE teacher_id <> (SELECT id FROM sys_user WHERE username = 'teacher_test');

-- 5.1 承接课件共享记录归属（共享广场记录随原课件作者归属到该教师）
UPDATE courseware_share
SET teacher_id = (SELECT id FROM sys_user WHERE username = 'teacher_test')
WHERE teacher_id <> (SELECT id FROM sys_user WHERE username = 'teacher_test');

-- 6. 学生测试账号加入全部班级（幂等：已在班级则跳过）
INSERT INTO enrollment (class_id, student_id, join_time)
SELECT c.id,
       (SELECT id FROM sys_user WHERE username = 'student_test'),
       NOW()
FROM class_info c
WHERE NOT EXISTS (
    SELECT 1 FROM enrollment e
    WHERE e.class_id = c.id
      AND e.student_id = (SELECT id FROM sys_user WHERE username = 'student_test')
);
