package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生个人成绩返回对象（每个任务一条）
 */
@Data
public class MyGradeVO {

    private Long taskId;
    private String taskTitle;
    private String className;
    private Integer totalScore;
    private Integer teacherScore;
    private String comment;

    /** REVIEWING 批阅中 / GRADED 已批阅 */
    private String status;

    private LocalDateTime submitTime;

    /** 本任务内班级排名 */
    private Integer rank;

    /** 本任务已提交学生总数 */
    private Integer totalStudents;
}
