package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师对共享课件的收藏记录
 */
@Data
public class CoursewareFavorite {

    private Long id;
    private Long teacherId;
    private Long shareId;
    private LocalDateTime createdAt;
}
