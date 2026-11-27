package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学任务返回对象（含班级名与课件标题）
 */
@Data
public class TaskVO {

    private Long id;
    private String title;
    private Long classId;
    private String className;
    private Long animationId;
    private String animationTitle;
    private LocalDateTime deadline;

    /** ACTIVE 进行中 / CLOSED 已截止 */
    private String status;

    private LocalDateTime createTime;

    /** 学生个人顺延补交截止（仅学生任务列表返回；无则为 null） */
    private LocalDateTime extendDeadline;
}
