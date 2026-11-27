package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生任务学习记录：记录首次进入课件时间、累计学习秒数与“我已学完”确认状态。
 * 用于“先学后练”：完成课件学习后才下发题目，提交后才展示参考答案。
 */
@Data
public class TaskStudyRecord {

    private Long id;
    private Long taskId;
    private Long studentId;

    /** 前端上报的学习秒数（服务端会以首次进入时间做下限校验） */
    private Integer studySeconds;

    /** 1 表示学生已确认学完 */
    private Integer confirmed;

    private LocalDateTime firstViewAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
