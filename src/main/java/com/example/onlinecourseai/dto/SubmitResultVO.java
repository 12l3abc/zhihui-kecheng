package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 提交作答后的结果汇总
 */
@Data
public class SubmitResultVO {

    /** 客观题自动判分得分 */
    private Integer objectiveScore;

    private Integer totalQuestions;
    private Integer answeredQuestions;

    /** REVIEWING 有主观题待批阅 / GRADED 全部自动判分完成 */
    private String status;
}
