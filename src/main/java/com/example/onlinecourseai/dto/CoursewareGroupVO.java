package com.example.onlinecourseai.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生端课件目录分组对象：按学科智能归类后的一组课件。
 *
 * 分组键为学科（由 CourseSubjectClassifier 依据标题/知识点/摘要判定），
 * 同一学科下挂该学生可见的全部课件。
 */
@Data
public class CoursewareGroupVO {

    /** 学科名称（数学 / 物理 / 化学 / 生物 / 信息技术 / 历史 / 地理 / 语文 / 英语 / 综合） */
    private String subject;

    /** 本组课件数量 */
    private Integer count;

    /** 本组课件列表 */
    private List<CoursewareItem> coursewares;

    @Data
    public static class CoursewareItem {
        /** 课件（animation）主键 */
        private Long animationId;
        /** 课件标题 */
        private String title;
        /** 知识点 */
        private String knowledgePoint;
        /** 内容摘要 */
        private String summary;
        /** 关联班级（首个引用任务所在班级） */
        private String className;
        /** 创建时间 */
        private LocalDateTime createdAt;
    }
}
