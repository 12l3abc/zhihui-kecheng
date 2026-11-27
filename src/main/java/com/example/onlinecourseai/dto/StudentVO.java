package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级学生名单返回对象（班级+学生信息）
 */
@Data
public class StudentVO {

    private Long id;
    private String username;
    private String realName;
    private String gender;
    private String grade;
    private String major;
    private LocalDateTime joinTime;
}
