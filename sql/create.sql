-- ============================================
-- 基于 Java 的线上教学 AI 动画课件生成系统
-- 数据库初始化脚本（MySQL 8.0，utf8mb4）
-- 执行方式：mysql -u root -p < create.sql
-- ============================================

CREATE DATABASE IF NOT EXISTS course_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE course_ai;

-- 1. 用户表（教师/学生共用）
CREATE TABLE sys_user (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL COMMENT '用户名',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
    role       VARCHAR(20)  NOT NULL COMMENT '角色: teacher/student',
    real_name  VARCHAR(50)  COMMENT '真实姓名',
    phone      VARCHAR(20)  COMMENT '手机号',
    email      VARCHAR(100) COMMENT '邮箱',
    gender     VARCHAR(10)  COMMENT '性别',
    grade      VARCHAR(20)  COMMENT '年级',
    major      VARCHAR(50)  COMMENT '专业',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 班级表（注意：class 是保留字，表名用 class_info）
CREATE TABLE class_info (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
    teacher_id BIGINT       NOT NULL COMMENT '创建班级的教师',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_class_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- 3. 选课表（学生-班级 多对多）
CREATE TABLE enrollment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id   BIGINT NOT NULL COMMENT '班级ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    join_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    CONSTRAINT fk_enroll_class  FOREIGN KEY (class_id)   REFERENCES class_info(id),
    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_enroll (class_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选课表';

-- 4. 课程表（教师所教科目，一个教师可带多门课）
CREATE TABLE course (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  BIGINT       NOT NULL COMMENT '所属教师',
    course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
    course_code VARCHAR(50)  DEFAULT '' COMMENT '课程代码',
    credit      INT          DEFAULT 2 COMMENT '学分',
    semester    VARCHAR(20)  DEFAULT '' COMMENT '学期',
    description VARCHAR(500) DEFAULT '' COMMENT '课程说明',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 5. 课件表（存大模型生成的动画 HTML）
CREATE TABLE animation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200) NOT NULL COMMENT '课件标题',
    knowledge_point TEXT COMMENT '知识点描述',
    summary         TEXT COMMENT '重点知识提炼摘要',
    content_html    LONGTEXT COMMENT '大模型生成的动画HTML代码',
    status          VARCHAR(20) DEFAULT 'GENERATING' COMMENT '状态: GENERATING/SUCCESS/FAILED',
    teacher_id      BIGINT NOT NULL COMMENT '生成课件的教师',
    course_id       BIGINT NULL COMMENT '归属课程（智能归科，NULL=未归科）',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_animation_course (course_id),
    CONSTRAINT fk_anim_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件表';

-- 6. 教学任务表（教师发布到班级）
CREATE TABLE task (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL COMMENT '任务标题',
    class_id     BIGINT NOT NULL COMMENT '目标班级',
    animation_id BIGINT NOT NULL COMMENT '关联课件',
    teacher_id   BIGINT COMMENT '任务发布者(教师)，按真实发布者过滤',
    deadline     DATETIME COMMENT '截止时间',
    status       VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/CLOSED',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    UNIQUE KEY uk_task_class_animation (class_id, animation_id),
    CONSTRAINT fk_task_class     FOREIGN KEY (class_id)     REFERENCES class_info(id),
    CONSTRAINT fk_task_animation FOREIGN KEY (animation_id) REFERENCES animation(id),
    CONSTRAINT fk_task_teacher   FOREIGN KEY (teacher_id)  REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教学任务表';

-- 7. 题目表（课件配套练习）
CREATE TABLE question (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    animation_id   BIGINT NOT NULL COMMENT '所属课件',
    question_text  VARCHAR(500) NOT NULL COMMENT '题干',
    options        TEXT COMMENT '选项(JSON数组，客观题用)',
    answer         VARCHAR(50) COMMENT '标准答案',
    question_type  VARCHAR(20) NOT NULL COMMENT '类型: objective/subjective',
    score          INT DEFAULT 10 COMMENT '分值',
    CONSTRAINT fk_question_anim FOREIGN KEY (animation_id) REFERENCES animation(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 8. 作答记录表（学生逐题作答）
CREATE TABLE answer_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id      BIGINT NOT NULL COMMENT '任务ID',
    question_id  BIGINT NOT NULL COMMENT '题目ID',
    student_id   BIGINT NOT NULL COMMENT '学生ID',
    answer_text  TEXT COMMENT '学生答案',
    is_correct   TINYINT COMMENT '客观题是否答对: 1对/0错，主观题为NULL',
    score        INT DEFAULT 0 COMMENT '本题得分',
    submit_time  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    CONSTRAINT fk_ans_task     FOREIGN KEY (task_id)     REFERENCES task(id),
    CONSTRAINT fk_ans_question FOREIGN KEY (question_id) REFERENCES question(id),
    CONSTRAINT fk_ans_student  FOREIGN KEY (student_id)  REFERENCES sys_user(id),
    UNIQUE KEY uk_ans (task_id, question_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作答记录表';

-- 9. 成绩表（每任务每学生一条汇总）
CREATE TABLE grade (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT NOT NULL COMMENT '任务ID',
    student_id    BIGINT NOT NULL COMMENT '学生ID',
    total_score   INT DEFAULT 0 COMMENT '总分',
    teacher_score INT DEFAULT 0 COMMENT '主观题得分',
    comment       VARCHAR(500) COMMENT '教师评语',
    status        VARCHAR(20) DEFAULT 'REVIEWING' COMMENT '状态: REVIEWING批阅中/GRADED已批阅',
    submit_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    CONSTRAINT fk_grade_task    FOREIGN KEY (task_id)    REFERENCES task(id),
    CONSTRAINT fk_grade_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_grade (task_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成绩表';

-- 10. 学生课件学习记录（先学后练：有效停留时长 + 手动确认）
CREATE TABLE task_study_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id       BIGINT NOT NULL COMMENT '任务ID',
    student_id    BIGINT NOT NULL COMMENT '学生ID',
    study_seconds INT NOT NULL DEFAULT 0 COMMENT '服务端累计的有效学习秒数',
    confirmed     TINYINT NOT NULL DEFAULT 0 COMMENT '1表示学生已确认学完',
    first_view_at DATETIME NULL COMMENT '首次打开课件时间',
    confirmed_at  DATETIME NULL COMMENT '确认学完时间',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_student (task_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生课件学习记录表';

-- 11. 课件评价与疑问表（学生对动画课件的反馈）
CREATE TABLE courseware_feedback (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    animation_id BIGINT       NOT NULL COMMENT '关联课件ID',
    student_id   BIGINT       NOT NULL COMMENT '学生ID',
    student_name VARCHAR(50)  COMMENT '学生姓名（冗余）',
    type         VARCHAR(20)  NOT NULL COMMENT '类型: evaluation评价 / question疑问',
    content      TEXT         NOT NULL COMMENT '评价或疑问内容',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    CONSTRAINT fk_fb_animation FOREIGN KEY (animation_id) REFERENCES animation(id),
    CONSTRAINT fk_fb_student   FOREIGN KEY (student_id)   REFERENCES sys_user(id),
    INDEX idx_fb_animation (animation_id),
    INDEX idx_fb_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件评价与疑问表';

-- 12. 课件共享表（教师选择性发布到共享广场）
CREATE TABLE courseware_share (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    animation_id   BIGINT       NOT NULL COMMENT '被共享的课件',
    teacher_id     BIGINT       NOT NULL COMMENT '课件原作者',
    subject        VARCHAR(30)  NOT NULL COMMENT '学科',
    note           VARCHAR(500) COMMENT '共享说明',
    status         TINYINT      NOT NULL DEFAULT 1 COMMENT '1已发布 0已下架',
    view_count     INT          NOT NULL DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT          NOT NULL DEFAULT 0 COMMENT '收藏次数',
    published_at   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_share_animation (animation_id),
    KEY idx_share_subject (subject, status, published_at),
    CONSTRAINT fk_share_animation FOREIGN KEY (animation_id) REFERENCES animation(id) ON DELETE CASCADE,
    CONSTRAINT fk_share_teacher   FOREIGN KEY (teacher_id)   REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件共享表';

-- 13. 课件收藏表（教师收藏共享课件）
CREATE TABLE courseware_favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '收藏教师',
    share_id   BIGINT NOT NULL COMMENT '共享记录',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_favorite_teacher_share (teacher_id, share_id),
    KEY idx_favorite_share (share_id),
    CONSTRAINT fk_favorite_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
    CONSTRAINT fk_favorite_share   FOREIGN KEY (share_id)   REFERENCES courseware_share(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件收藏表';

-- 14. 学生单独补交延期表（教师打回重做时为某生单独顺延的补交截止窗口，不影响全班）
CREATE TABLE task_submit_extension (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT      NOT NULL COMMENT '任务ID',
    student_id      BIGINT      NOT NULL COMMENT '学生ID',
    extend_deadline DATETIME    NOT NULL COMMENT '顺延的补交截止时间',
    created_at      DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_ext_task_student (task_id, student_id),
    CONSTRAINT fk_ext_task    FOREIGN KEY (task_id)    REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT fk_ext_student FOREIGN KEY (student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生单独补交延期表';

-- 15. 教师催交提醒表（教师对未提交学生催交，学生进入任务时看到提醒）
CREATE TABLE task_remind (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT       NOT NULL COMMENT '任务ID',
    student_id BIGINT       NOT NULL COMMENT '学生ID',
    teacher_id BIGINT       NOT NULL COMMENT '催交教师ID',
    message    VARCHAR(200) COMMENT '催交文案',
    read_flag  TINYINT DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '催交时间',
    CONSTRAINT fk_remind_task    FOREIGN KEY (task_id)    REFERENCES task(id) ON DELETE CASCADE,
    CONSTRAINT fk_remind_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
    CONSTRAINT fk_remind_teacher  FOREIGN KEY (teacher_id)  REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师催交提醒表';

-- 16. 教师通知表（学生提交作业、学生反馈等事件推送给发布教师）
CREATE TABLE teacher_notification (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id     BIGINT       NOT NULL COMMENT '接收通知的教师(发布者)',
    type           VARCHAR(20)  NOT NULL COMMENT '类型: SUBMIT作业提交 / FEEDBACK课件反馈',
    title          VARCHAR(200) NOT NULL COMMENT '通知标题',
    content        VARCHAR(500) COMMENT '通知正文',
    ref_task_id    BIGINT       COMMENT '关联任务ID(SUBMIT)',
    ref_student_id BIGINT       COMMENT '关联学生ID',
    ref_animation_id BIGINT     COMMENT '关联课件ID(FEEDBACK)',
    read_flag      TINYINT DEFAULT 0 COMMENT '是否已读: 0未读/1已读',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '通知时间',
    INDEX idx_teacher_read (teacher_id, read_flag),
    CONSTRAINT fk_notify_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师通知表';
