package com.example.onlinecourseai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能归科结果：把教师已有课件按科目聚合，并给出新建/复用的课程
 */
@Data
public class CourseAnalysisResult {

    /** 参与分析的课件总数 */
    private int analyzed;

    /** 成功写入课程归属的课件数 */
    private int assigned;

    /** 本次新建的课程数 */
    private int createdCourses;

    /** 按科目聚合的分组 */
    private List<Group> groups = new ArrayList<>();

    @Data
    public static class Group {
        private Long courseId;
        private String courseName;
        private String courseCode;
        /** true=本次新建，false=复用已有课程 */
        private boolean created;
        private int count;
        private List<Item> items = new ArrayList<>();
    }

    @Data
    public static class Item {
        private Long coursewareId;
        private String title;
        private String subject;
        /** 识别得分，越大越可信 */
        private int score;
        /** 命中的关键词，用于向教师解释判定依据 */
        private List<String> hits = new ArrayList<>();
    }
}
