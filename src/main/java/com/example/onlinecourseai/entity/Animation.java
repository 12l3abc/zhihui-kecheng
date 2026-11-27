package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课件表 animation（存大模型生成的动画 HTML）
 */
@Data
public class Animation {

    private Long id;
    private String title;
    private String knowledgePoint;
    private String summary;
    private String contentHtml;

    /** 状态：GENERATING 生成中 / SUCCESS 成功 / FAILED 失败 */
    private String status;

    private Long teacherId;

    /** 所属课程（course 表 id）；未归科时为空 */
    private Long courseId;

    private LocalDateTime createdAt;
}
