package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 班级表 class_info（class 是 MySQL 保留字，表名用 class_info）
 */
@Data
public class ClassInfo {

    private Long id;
    private String className;
    private Long teacherId;
    private LocalDateTime createdAt;

    /** 班级学生数（列表查询带出，非表字段） */
    private Integer studentCount;
}
