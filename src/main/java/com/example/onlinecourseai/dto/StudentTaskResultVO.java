package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生查看自己某次任务作答的结果（提交后才返回参考答案）
 */
@Data
public class StudentTaskResultVO {

    /** 是否已提交；未提交时不返回任何题目答案 */
    private boolean submitted;

    /** 客观题自动判分得分 */
    private Integer objectiveScore;

    private Integer teacherScore;
    private Integer totalScore;
    private String comment;

    /** REVIEWING 批阅中 / GRADED 已完成 */
    private String status;

    private LocalDateTime submitTime;

    /** 累计学习时长（秒），无记录为 null */
    private Integer studySeconds;

    /** 首次进入学习时间 */
    private LocalDateTime firstViewAt;

    /** 逐题作答与参考答案 */
    private List<StudentAnswerVO> answers;
}
