package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作答记录表 answer_record（学生逐题作答）
 */
@Data
public class AnswerRecord {

    private Long id;
    private Long taskId;
    private Long questionId;
    private Long studentId;
    private String answerText;

    /** 客观题是否正确：1 对 / 0 错；主观题为 null（等待教师批阅） */
    private Integer isCorrect;

    private Integer score;
    private LocalDateTime submitTime;
}
