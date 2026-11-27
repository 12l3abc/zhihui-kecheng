package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 教师批阅总览（任务下每个学生的提交与得分情况）
 */
@Data
public class ReviewStudentVO {

    private Long studentId;
    private String username;
    private String realName;

    /** 1 已提交 / 0 未提交 */
    private Integer submitted;

    /** 客观题得分 */
    private Integer objectiveScore;

    /** 主观题得分（未批阅为 0） */
    private Integer teacherScore;

    /** 总分（客观 + 主观） */
    private Integer totalScore;

    /** 教师评语（已批阅有值，用于重新批阅预填） */
    private String comment;

    /** REVIEWING 批阅中 / GRADED 已批阅 / 空串未提交 */
    private String status;

    /** 成绩排名（仅已批阅学生有值，未提交/批阅中为 null） */
    private Integer rank;

    /** 教师给该生单独顺延的补交截止时间（无则为 null） */
    private java.time.LocalDateTime extendDeadline;
}
