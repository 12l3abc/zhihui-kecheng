package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务详情返回对象（任务信息 + 课件动画 + 配套题目）
 */
@Data
public class TaskDetailVO {

    private TaskVO task;

    /** 课件完整 HTML（前端 iframe srcdoc 展示） */
    private String contentHtml;

    /** 配套练习题；学生未确认学完课件时为空 */
    private List<QuestionVO> questions;

    /** 当前学生是否已确认学完该任务课件（教师端恒为 true） */
    private Boolean studied;

    /** 当前学生已停留的秒数 */
    private Integer studySeconds;

    /** 解锁作答所需的最少停留秒数 */
    private Integer minStudySeconds;

    /** 当前学生的单独补交截止（教师打回顺延时有值，无则 null） */
    private LocalDateTime extendDeadline;
}
