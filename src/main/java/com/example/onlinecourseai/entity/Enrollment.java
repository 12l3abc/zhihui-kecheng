package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 选课表 enrollment（学生-班级 多对多）
 */
@Data
public class Enrollment {

    private Long id;
    private Long classId;
    private Long studentId;
    private LocalDateTime joinTime;
}
