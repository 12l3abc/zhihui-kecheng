package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 共享广场卡片数据
 */
@Data
public class SharedCoursewareVO {

    /** 共享记录 ID */
    private Long id;

    /** 原课件 ID，用于预览 */
    private Long animationId;

    private Long ownerId;
    private String ownerName;

    private String title;
    private String summary;
    private String knowledgePoint;

    private String subject;
    private String note;

    /** 1 已发布 / 0 已下架 */
    private Integer status;

    private Integer viewCount;
    private Integer favoriteCount;
    private Integer questionCount;

    /** 当前登录教师是否已收藏 */
    private Boolean favorited;

    /** 是否是当前教师自己发布的 */
    private Boolean mine;

    private LocalDateTime publishedAt;
}
