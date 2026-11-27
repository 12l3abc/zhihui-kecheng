package com.example.onlinecourseai.dto;

import lombok.Data;
import java.util.List;

/**
 * PPT 结构化大纲（方案B：大模型输出统一 JSON 结构）
 * 支持多种课件类型：时间轴、概念讲解、步骤演示、对比分析、随堂练习
 */
@Data
public class PptxOutline {

    private String title;
    private String subtitle;
    private List<Slide> slides;

    @Data
    public static class Slide {
        /** 类型：cover / content / timeline / steps / comparison / quiz */
        private String type;
        private String heading;
        private String subheading;
        /** content 类型用：要点列表 */
        private List<String> points;
        /** timeline 类型用：时间轴事件 */
        private List<TimelineEvent> events;
        /** steps 类型用：步骤项 */
        private List<StepItem> steps;
        /** comparison 类型用：对比项（左右两列） */
        private List<CompareItem> comparison;
        /** quiz 类型用：习题 */
        private Quiz quiz;
        /** 是否需要点击逐步显示的交互动画 */
        private boolean interactive;
    }

    @Data
    public static class TimelineEvent {
        private String year;
        private String title;
        private String desc;
    }

    @Data
    public static class StepItem {
        private String number;
        private String title;
        private String desc;
    }

    @Data
    public static class CompareItem {
        private String left;
        private String right;
    }

    @Data
    public static class Quiz {
        private String question;
        private List<String> options;
        private String answer;
        private String explanation;
    }
}
