package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课件返回对象（AI 生成结果）
 */
@Data
public class CoursewareVO {

    private Long id;
    private String title;
    private String summary;

    /** 所属课程 ID（可空，表示尚未归科） */
    private Long courseId;

    /** 智能归类学科（教师端列表按学科分组用） */
    private String subject;

    /** 大模型生成的完整 HTML 动画代码（前端用 iframe 展示） */
    private String contentHtml;

    /** 状态：SUCCESS / FAILED */
    private String status;

    private LocalDateTime createdAt;

    /** 配套练习题 */
    private List<QuestionVO> questions;
}
