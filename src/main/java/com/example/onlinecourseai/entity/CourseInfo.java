package com.example.onlinecourseai.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 课程表 course（教师所教的科目，一个老师可带多门课）
 */
@Data
public class CourseInfo {

    private Long id;
    private Long teacherId;
    private String courseName;
    private String courseCode;
    private Integer credit;
    private String semester;
    private String description;
    private LocalDateTime createdAt;

    /** 该课程下的课件数量（非数据库字段，查询时回填） */
    private Integer coursewareCount;
}
