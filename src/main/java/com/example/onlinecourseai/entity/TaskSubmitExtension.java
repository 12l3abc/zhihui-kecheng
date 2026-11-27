package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生单独补交延期表（教师打回重做时为某生单独顺延的补交截止窗口，不影响全班）
 */
@Data
public class TaskSubmitExtension {

    private Long id;
    private Long taskId;
    private Long studentId;

    /** 顺延的补交截止时间 */
    private LocalDateTime extendDeadline;

    private LocalDateTime createdAt;
}
