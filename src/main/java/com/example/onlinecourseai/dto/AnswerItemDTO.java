package com.example.onlinecourseai.dto;

import lombok.Data;

/**
 * 单题作答内容
 */
@Data
public class AnswerItemDTO {

    private Long questionId;
    private String answerText;
}
