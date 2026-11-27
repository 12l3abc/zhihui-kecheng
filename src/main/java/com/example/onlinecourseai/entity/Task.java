package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学任务表 task（教师发布到班级）
 */
@Data
public class Task {

    private Long id;
    private String title;
    private Long classId;
    private Long animationId;
    private LocalDateTime deadline;

    /** 任务发布者（教师），按真实发布者过滤，避免按班级归属串出他人任务 */
    private Long teacherId;

    /** 状态：ACTIVE 进行中 / CLOSED 已截止 */
    private String status;

    private LocalDateTime createTime;
}
