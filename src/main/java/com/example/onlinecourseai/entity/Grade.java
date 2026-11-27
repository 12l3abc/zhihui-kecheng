package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 成绩表 grade（每任务每学生一条汇总）
 */
@Data
public class Grade {

    private Long id;
    private Long taskId;
    private Long studentId;
    private Integer totalScore;
    private Integer teacherScore;
    private String comment;

    /** 状态：REVIEWING 批阅中 / GRADED 已批阅 */
    private String status;

    private LocalDateTime submitTime;
}
