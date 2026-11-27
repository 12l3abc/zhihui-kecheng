package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 课件配套练习题返回对象
 */
@Data
public class QuestionVO {

    private Long id;
    private String questionText;

    /** 选项，JSON 数组字符串，如 ["A. xxx","B. xxx","C. xxx","D. xxx"] */
    private String options;

    private String answer;

    /** objective 客观题 / subjective 主观题 */
    private String questionType;

    private Integer score;
}
