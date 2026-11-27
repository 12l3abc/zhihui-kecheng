package com.example.onlinecourseai.entity;

import lombok.Data;

/**
 * 题目表 question（课件配套练习）
 */
@Data
public class Question {

    private Long id;
    private Long animationId;
    private String questionText;

    /** 选项，JSON 数组字符串（客观题用），例如 ["A. 选项1","B. 选项2","C. 选项3","D. 选项4"] */
    private String options;

    private String answer;

    /** 类型：objective 客观题 / subjective 主观题 */
    private String questionType;

    private Integer score;
}
