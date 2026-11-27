package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 教师查看某学生作答详情（单题）
 */
@Data
public class StudentAnswerVO {

    private Long questionId;
    private String questionText;
    private String options;

    /** objective 客观题 / subjective 主观题 */
    private String questionType;

    private String studentAnswer;
    private String correctAnswer;

    /** 1 对 / 0 错 / null 主观题待批阅 */
    private Integer isCorrect;

    /** 本题实际得分（客观题自动判分，主观题由教师评分汇总） */
    private Integer score;

    /** 本题满分 */
    private Integer questionScore;
}
