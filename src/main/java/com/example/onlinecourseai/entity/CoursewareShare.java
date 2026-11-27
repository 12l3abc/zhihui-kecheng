package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课件共享记录：教师把自己的课件发布到共享广场。
 * 与 animation.status 解耦，status 只表示“是否在广场展示”。
 */
@Data
public class CoursewareShare {

    private Long id;

    /** 被共享的课件 */
    private Long animationId;

    /** 课件原作者，权限校验用 */
    private Long teacherId;

    /** 学科，例如“英语”“物理” */
    private String subject;

    /** 共享说明，教师可选填 */
    private String note;

    /** 1 已发布 / 0 已下架 */
    private Integer status;

    private Integer viewCount;

    private Integer favoriteCount;

    private LocalDateTime publishedAt;

    private LocalDateTime updatedAt;
}
