package com.example.onlinecourseai.dto;

import lombok.Data;

import java.util.List;

/**
 * 学生提交整份作答的请求体
 */
@Data
public class AnswerSubmitDTO {

    private List<AnswerItemDTO> answers;
}
