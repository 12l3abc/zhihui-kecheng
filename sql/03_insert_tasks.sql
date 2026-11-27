USE course_ai;

-- 为每个班级新增教学任务（算法为主，辅以数理基础）
-- 班级1（软件工程23-1班）已有任务1(二分基础)、任务3(二分进阶)，新增2个
INSERT INTO task (title, class_id, animation_id, deadline, status, create_time) VALUES
('快速排序算法练习', 1, 9, '2026-09-25 23:59:00', 'ACTIVE', '2026-09-15 10:00:00'),
('牛顿第二定律应用练习', 1, 8, '2026-10-08 23:59:00', 'ACTIVE', '2026-09-28 10:00:00');

-- 班级2（软件工程23-2班）已有任务2(二分基础)，新增3个
INSERT INTO task (title, class_id, animation_id, deadline, status, create_time) VALUES
('二分查找算法进阶练习', 2, 2, '2026-09-20 23:59:00', 'ACTIVE', '2026-09-10 10:00:00'),
('快速排序算法练习', 2, 9, '2026-09-28 23:59:00', 'ACTIVE', '2026-09-18 10:00:00'),
('牛顿第二定律应用练习', 2, 8, '2026-10-10 23:59:00', 'ACTIVE', '2026-09-30 10:00:00');

-- 班级3（软件工程23-3班）已有任务4(二分实战)，新增3个
INSERT INTO task (title, class_id, animation_id, deadline, status, create_time) VALUES
('二分查找算法基础练习', 3, 1, '2026-09-18 23:59:00', 'ACTIVE', '2026-09-08 10:00:00'),
('快速排序算法练习', 3, 9, '2026-09-26 23:59:00', 'ACTIVE', '2026-09-16 10:00:00'),
('牛顿第二定律应用练习', 3, 8, '2026-10-08 23:59:00', 'ACTIVE', '2026-09-28 10:00:00');

-- 班级4（数据科学23-1班）无任务，新增4个
INSERT INTO task (title, class_id, animation_id, deadline, status, create_time) VALUES
('二分查找算法基础练习', 4, 1, '2026-09-18 23:59:00', 'ACTIVE', '2026-09-08 10:00:00'),
('快速排序算法练习', 4, 9, '2026-09-26 23:59:00', 'ACTIVE', '2026-09-16 10:00:00'),
('牛顿第二定律应用练习', 4, 8, '2026-10-08 23:59:00', 'ACTIVE', '2026-09-28 10:00:00'),
('勾股定理应用练习', 4, 4, '2026-10-15 23:59:00', 'ACTIVE', '2026-10-01 10:00:00');

SELECT '=== 任务列表 ===' AS info;
SELECT t.id, t.title, c.class_name, a.title AS courseware, t.status 
FROM task t JOIN class_info c ON t.class_id=c.id JOIN animation a ON t.animation_id=a.id 
ORDER BY t.class_id, t.id;
