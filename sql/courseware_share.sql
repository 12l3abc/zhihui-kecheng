-- 课件共享：共享广场与收藏
-- 与 animation.status 解耦，status 只表示是否在共享广场展示

CREATE TABLE IF NOT EXISTS courseware_share (
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

CREATE TABLE IF NOT EXISTS courseware_favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    teacher_id BIGINT NOT NULL COMMENT '收藏教师',
    share_id   BIGINT NOT NULL COMMENT '共享记录',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_favorite_teacher_share (teacher_id, share_id),
    KEY idx_favorite_share (share_id),
    CONSTRAINT fk_favorite_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
    CONSTRAINT fk_favorite_share   FOREIGN KEY (share_id)   REFERENCES courseware_share(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课件收藏表';
