package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师催交提醒表（教师对未提交学生催交，学生进入任务时看到提醒）
 */
@Data
public class TaskRemind {

    private Long id;
    private Long taskId;
    private Long studentId;
    private Long teacherId;

    /** 催交文案（预留，默认由前端固定展示） */
    private String message;

    /** 是否已读：0未读 / 1已读 */
    private Integer readFlag;

    private LocalDateTime createdAt;
}
