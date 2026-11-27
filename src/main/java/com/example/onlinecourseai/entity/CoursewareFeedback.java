package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课件评价与疑问表 courseware_feedback
 * 学生对动画课件发表评价或提出疑问
 */
@Data
public class CoursewareFeedback {

    private Long id;

    /** 关联课件ID */
    private Long animationId;

    /** 学生ID */
    private Long studentId;

    /** 学生姓名（冗余，方便展示） */
    private String studentName;

    /** 类型：evaluation 评价 / question 疑问 */
    private String type;

    /** 评价或疑问内容 */
    private String content;

    private LocalDateTime createdAt;

    /** 学生所在班级名称（非数据库字段，JOIN查询返回） */
    private String className;
}
