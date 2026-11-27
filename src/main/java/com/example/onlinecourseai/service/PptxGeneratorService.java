package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.PptxOutline;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 方案B：基于结构化大纲生成 PPTX
 * 支持多种课件类型，一页式布局，点击逐步显示交互动画
 */
@Service
public class PptxGeneratorService {

    private static final Color PRIMARY = new Color(196, 30, 58);
    private static final Color DARK = new Color(30, 30, 30);
    private static final Color GRAY = new Color(100, 100, 100);
    private static final Color LIGHT_BG = new Color(250, 248, 245);
    private static final Color ACCENT_GOLD = new Color(212, 175, 55);
    private static final Color BLUE = new Color(50, 100, 180);
    private static final String FONT = "微软雅黑";

    /**
     * 基于结构化大纲生成 PPTX
     */
    public byte[] generateFromOutline(PptxOutline outline) throws Exception {
        XMLSlideShow ppt = new XMLSlideShow();
        ppt.setPageSize(new java.awt.Dimension(1280, 720));

        if (outline == null || outline.getSlides() == null || outline.getSlides().isEmpty()) {
            XSLFSlide slide = ppt.createSlide();
            setSlideBg(slide, LIGHT_BG);
            addText(slide, "暂无内容", 400, 300, 480, 80, 32, DARK, true);
            return toBytes(ppt);
        }

        for (PptxOutline.Slide slideData : outline.getSlides()) {
            XSLFSlide slide = ppt.createSlide();
            setSlideBg(slide, LIGHT_BG);

            String type = slideData.getType() != null ? slideData.getType() : "content";
            switch (type) {
                case "cover": renderCover(slide, slideData, outline.getTitle()); break;
                case "timeline": renderTimeline(slide, slideData); break;
                case "steps": renderSteps(slide, slideData); break;
                case "comparison": renderComparison(slide, slideData); break;
                case "quiz": renderQuiz(slide, slideData); break;
                default: renderContent(slide, slideData); break;
            }
        }
        return toBytes(ppt);
    }

    // ==================== 封面页 ====================
    private void renderCover(XSLFSlide slide, PptxOutline.Slide data, String fallbackTitle) {
        String title = data.getHeading() != null ? data.getHeading() : fallbackTitle;
        String subtitle = data.getSubheading() != null ? data.getSubheading() : "";

        // 左侧红色装饰条
        XSLFAutoShape bar = slide.createAutoShape();
        bar.setShapeType(ShapeType.RECT);
        bar.setAnchor(new Rectangle(80, 180, 12, 360));
        bar.setFillColor(PRIMARY);
        bar.setLineColor(PRIMARY);

        addText(slide, title, 130, 220, 1000, 120, 54, DARK, true);
        if (!subtitle.isEmpty()) {
            addText(slide, subtitle, 130, 360, 1000, 60, 24, GRAY, false);
        }

        XSLFAutoShape line = slide.createAutoShape();
        line.setShapeType(ShapeType.RECT);
        line.setAnchor(new Rectangle(130, 460, 200, 4));
        line.setFillColor(ACCENT_GOLD);
        line.setLineColor(ACCENT_GOLD);

        addText(slide, "智能动画课件生成平台", 900, 640, 300, 30, 14, GRAY, false);
    }

    // ==================== 内容讲解页 ====================
    private void renderContent(XSLFSlide slide, PptxOutline.Slide data) {
        addPageHeader(slide, data.getHeading());
        List<String> points = data.getPoints() != null ? data.getPoints() : new ArrayList<>();

        int startY = 160;
        int spacing = Math.min(100, 480 / Math.max(points.size(), 1));

        for (int i = 0; i < points.size(); i++) {
            int y = startY + i * spacing;
            // 序号圆点
            XSLFAutoShape dot = slide.createAutoShape();
            dot.setShapeType(ShapeType.ELLIPSE);
            dot.setAnchor(new Rectangle(100, y + 5, 36, 36));
            dot.setFillColor(PRIMARY);
            dot.setLineColor(PRIMARY);
            setShapeText(dot, String.valueOf(i + 1), 16, Color.WHITE, true, TextParagraph.TextAlign.CENTER);

            // 要点文字
            addText(slide, points.get(i), 160, y, 1000, 50, 22, DARK, false);
        }

        if (data.isInteractive() && !points.isEmpty()) {
            addClickHint(slide, 580);
        }
    }

    // ==================== 时间轴页（一页式） ====================
    private void renderTimeline(XSLFSlide slide, PptxOutline.Slide data) {
        addPageHeader(slide, data.getHeading());
        List<PptxOutline.TimelineEvent> events = data.getEvents() != null ? data.getEvents() : new ArrayList<>();
        if (events.isEmpty()) return;

        int lineY = 380;
        XSLFAutoShape timelineLine = slide.createAutoShape();
        timelineLine.setShapeType(ShapeType.RECT);
        timelineLine.setAnchor(new Rectangle(80, lineY, 1120, 4));
        timelineLine.setFillColor(PRIMARY);
        timelineLine.setLineColor(PRIMARY);

        int count = events.size();
        int availableWidth = 1080;
        int stepX = count > 1 ? availableWidth / (count - 1) : 0;

        for (int i = 0; i < count; i++) {
            PptxOutline.TimelineEvent ev = events.get(i);
            int x = 100 + i * stepX;
            boolean isLeft = i % 2 == 0;
            int cardY = isLeft ? 160 : 430;

            // 节点圆点
            XSLFAutoShape node = slide.createAutoShape();
            node.setShapeType(ShapeType.ELLIPSE);
            node.setAnchor(new Rectangle(x - 12, lineY - 10, 28, 28));
            node.setFillColor(PRIMARY);
            node.setLineColor(Color.WHITE);
            node.setLineWidth(3.0);

            // 年份
            addText(slide, ev.getYear() != null ? ev.getYear() : "",
                    x - 60, isLeft ? 300 : lineY + 20, 120, 35, 18, PRIMARY, true);

            // 事件卡片
            XSLFAutoShape card = slide.createAutoShape();
            card.setShapeType(ShapeType.RECT);
            card.setAnchor(new Rectangle(x - 85, cardY, 170, 120));
            card.setFillColor(Color.WHITE);
            card.setLineColor(new Color(230, 230, 230));
            card.setLineWidth(1.0);

            // 卡片标题
            List<XSLFTextParagraph> ps = card.getTextParagraphs();
            XSLFTextParagraph tp = ps.isEmpty() ? card.addNewTextParagraph() : ps.get(0);
            tp.setTextAlign(TextParagraph.TextAlign.CENTER);
            XSLFTextRun tr = tp.addNewTextRun();
            tr.setText(ev.getTitle() != null ? ev.getTitle() : "");
            tr.setFontSize(15.0);
            tr.setBold(true);
            tr.setFontColor(DARK);
            tr.setFontFamily(FONT);

            // 卡片描述
            if (ev.getDesc() != null && !ev.getDesc().isEmpty()) {
                XSLFTextParagraph tp2 = card.addNewTextParagraph();
                tp2.setTextAlign(TextParagraph.TextAlign.CENTER);
                tp2.setSpaceBefore(6.0);
                XSLFTextRun tr2 = tp2.addNewTextRun();
                String desc = ev.getDesc();
                if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                tr2.setText(desc);
                tr2.setFontSize(11.0);
                tr2.setFontColor(GRAY);
                tr2.setFontFamily(FONT);
            }
        }

        if (data.isInteractive() && !events.isEmpty()) {
            addClickHint(slide, 640);
        }
    }

    // ==================== 步骤演示页（一页式） ====================
    private void renderSteps(XSLFSlide slide, PptxOutline.Slide data) {
        addPageHeader(slide, data.getHeading());
        List<PptxOutline.StepItem> steps = data.getSteps() != null ? data.getSteps() : new ArrayList<>();
        if (steps.isEmpty()) return;

        int count = steps.size();
        int cardWidth = count <= 3 ? 340 : (count <= 4 ? 260 : 210);
        int totalWidth = count * cardWidth + (count - 1) * 20;
        int startX = (1280 - totalWidth) / 2;

        for (int i = 0; i < count; i++) {
            PptxOutline.StepItem step = steps.get(i);
            int x = startX + i * (cardWidth + 20);

            XSLFAutoShape card = slide.createAutoShape();
            card.setShapeType(ShapeType.RECT);
            card.setAnchor(new Rectangle(x, 170, cardWidth, 400));
            card.setFillColor(Color.WHITE);
            card.setLineColor(new Color(230, 230, 230));
            card.setLineWidth(1.0);

            // 编号大圆
            XSLFAutoShape numCircle = slide.createAutoShape();
            numCircle.setShapeType(ShapeType.ELLIPSE);
            numCircle.setAnchor(new Rectangle(x + cardWidth / 2 - 35, 200, 70, 70));
            numCircle.setFillColor(PRIMARY);
            numCircle.setLineColor(PRIMARY);
            setShapeText(numCircle, step.getNumber() != null ? step.getNumber() : String.valueOf(i + 1),
                    28, Color.WHITE, true, TextParagraph.TextAlign.CENTER);

            addText(slide, step.getTitle() != null ? step.getTitle() : "",
                    x + 10, 290, cardWidth - 20, 40, 18, DARK, true);
            addText(slide, step.getDesc() != null ? step.getDesc() : "",
                    x + 15, 340, cardWidth - 30, 200, 13, GRAY, false);

            // 箭头
            if (i < count - 1) {
                XSLFAutoShape arrow = slide.createAutoShape();
                arrow.setShapeType(ShapeType.RIGHT_ARROW);
                arrow.setAnchor(new Rectangle(x + cardWidth + 2, 350, 16, 30));
                arrow.setFillColor(ACCENT_GOLD);
                arrow.setLineColor(ACCENT_GOLD);
            }
        }

        if (data.isInteractive() && !steps.isEmpty()) {
            addClickHint(slide, 620);
        }
    }

    // ==================== 对比分析页（一页式） ====================
    private void renderComparison(XSLFSlide slide, PptxOutline.Slide data) {
        addPageHeader(slide, data.getHeading());
        List<PptxOutline.CompareItem> items = data.getComparison() != null ? data.getComparison() : new ArrayList<>();

        addText(slide, "A", 180, 140, 100, 40, 28, PRIMARY, true);
        addText(slide, "B", 1000, 140, 100, 40, 28, BLUE, true);

        XSLFAutoShape vline = slide.createAutoShape();
        vline.setShapeType(ShapeType.RECT);
        vline.setAnchor(new Rectangle(638, 140, 4, 480));
        vline.setFillColor(new Color(220, 220, 220));
        vline.setLineColor(new Color(220, 220, 220));

        int startY = 200;
        int rowHeight = Math.min(80, 440 / Math.max(items.size(), 1));

        for (int i = 0; i < items.size(); i++) {
            PptxOutline.CompareItem item = items.get(i);
            int y = startY + i * rowHeight;
            addText(slide, item.getLeft() != null ? item.getLeft() : "",
                    100, y, 500, rowHeight - 10, 16, DARK, false);
            addText(slide, item.getRight() != null ? item.getRight() : "",
                    680, y, 500, rowHeight - 10, 16, DARK, false);
        }

        if (data.isInteractive() && !items.isEmpty()) {
            addClickHint(slide, 650);
        }
    }

    // ==================== 随堂练习页 ====================
    private void renderQuiz(XSLFSlide slide, PptxOutline.Slide data) {
        addPageHeader(slide, data.getHeading() != null ? data.getHeading() : "随堂练习");
        PptxOutline.Quiz quiz = data.getQuiz();
        if (quiz == null) return;

        addText(slide, "Q: " + (quiz.getQuestion() != null ? quiz.getQuestion() : ""),
                100, 140, 1080, 60, 22, DARK, true);

        List<String> options = quiz.getOptions() != null ? quiz.getOptions() : new ArrayList<>();
        String answer = quiz.getAnswer() != null ? quiz.getAnswer() : "";

        int startY = 230;
        for (int i = 0; i < options.size(); i++) {
            int y = startY + i * 70;
            boolean isCorrect = answer.length() > 0 && options.get(i).startsWith(answer);

            XSLFAutoShape optBox = slide.createAutoShape();
            optBox.setShapeType(ShapeType.RECT);
            optBox.setAnchor(new Rectangle(120, y, 1040, 55));
            optBox.setFillColor(isCorrect ? new Color(230, 255, 230) : Color.WHITE);
            optBox.setLineColor(isCorrect ? new Color(80, 180, 80) : new Color(220, 220, 220));
            optBox.setLineWidth(isCorrect ? 2.5 : 1.0);

            List<XSLFTextParagraph> ps = optBox.getTextParagraphs();
            XSLFTextParagraph tp = ps.isEmpty() ? optBox.addNewTextParagraph() : ps.get(0);
            XSLFTextRun tr = tp.addNewTextRun();
            tr.setText(options.get(i));
            tr.setFontSize(18.0);
            tr.setFontColor(isCorrect ? new Color(30, 120, 30) : DARK);
            tr.setBold(isCorrect);
            tr.setFontFamily(FONT);
        }

        int ansY = startY + options.size() * 70 + 20;
        if (!answer.isEmpty()) {
            XSLFAutoShape ansBox = slide.createAutoShape();
            ansBox.setShapeType(ShapeType.RECT);
            ansBox.setAnchor(new Rectangle(120, ansY, 1040, 80));
            ansBox.setFillColor(new Color(255, 248, 230));
            ansBox.setLineColor(ACCENT_GOLD);
            ansBox.setLineWidth(1.5);

            List<XSLFTextParagraph> ps = ansBox.getTextParagraphs();
            XSLFTextParagraph tp = ps.isEmpty() ? ansBox.addNewTextParagraph() : ps.get(0);
            XSLFTextRun tr = tp.addNewTextRun();
            tr.setText("正确答案：" + answer);
            tr.setFontSize(18.0);
            tr.setBold(true);
            tr.setFontColor(new Color(180, 120, 0));
            tr.setFontFamily(FONT);

            if (quiz.getExplanation() != null && !quiz.getExplanation().isEmpty()) {
                XSLFTextParagraph tp2 = ansBox.addNewTextParagraph();
                tp2.setSpaceBefore(6.0);
                XSLFTextRun tr2 = tp2.addNewTextRun();
                tr2.setText("解析：" + quiz.getExplanation());
                tr2.setFontSize(14.0);
                tr2.setFontColor(GRAY);
                tr2.setFontFamily(FONT);
            }
        }
    }

    // ==================== 公共方法 ====================

    private void addPageHeader(XSLFSlide slide, String heading) {
        XSLFAutoShape bar = slide.createAutoShape();
        bar.setShapeType(ShapeType.RECT);
        bar.setAnchor(new Rectangle(0, 0, 1280, 8));
        bar.setFillColor(PRIMARY);
        bar.setLineColor(PRIMARY);

        if (heading != null && !heading.isEmpty()) {
            addText(slide, heading, 80, 30, 1100, 60, 32, DARK, true);
            XSLFAutoShape underline = slide.createAutoShape();
            underline.setShapeType(ShapeType.RECT);
            underline.setAnchor(new Rectangle(80, 95, 80, 4));
            underline.setFillColor(PRIMARY);
            underline.setLineColor(PRIMARY);
        }
    }

    private XSLFTextBox addText(XSLFSlide slide, String text, int x, int y, int w, int h,
                                 int fontSize, Color color, boolean bold) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(x, y, w, h));
        List<XSLFTextParagraph> ps = box.getTextParagraphs();
        XSLFTextParagraph p = ps.isEmpty() ? box.addNewTextParagraph() : ps.get(0);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text != null ? text : "");
        r.setFontSize((double) fontSize);
        r.setFontColor(color);
        r.setBold(bold);
        r.setFontFamily(FONT);
        return box;
    }

    private void setShapeText(XSLFAutoShape shape, String text, int fontSize, Color color,
                               boolean bold, TextParagraph.TextAlign align) {
        List<XSLFTextParagraph> ps = shape.getTextParagraphs();
        XSLFTextParagraph p = ps.isEmpty() ? shape.addNewTextParagraph() : ps.get(0);
        p.setTextAlign(align);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(text != null ? text : "");
        r.setFontSize((double) fontSize);
        r.setFontColor(color);
        r.setBold(bold);
        r.setFontFamily(FONT);
    }

    private void addClickHint(XSLFSlide slide, int y) {
        addText(slide, "▶ 点击鼠标逐步显示内容", 480, y, 320, 30, 14, GRAY, false);
    }

    private void setSlideBg(XSLFSlide slide, Color color) {
        // 用全屏矩形模拟背景（每个 slide 最先创建，天然位于底层，无需重排；
        // 注意：POI 不允许对同一个 shape 执行 remove 后再次 addShape，会报
        // "Adding a shape from a different container is not supported"）
        XSLFAutoShape bg = slide.createAutoShape();
        bg.setShapeType(ShapeType.RECT);
        bg.setAnchor(new Rectangle(0, 0, 1280, 720));
        bg.setFillColor(color);
        bg.setLineColor(color);
    }

    private byte[] toBytes(XMLSlideShow ppt) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ppt.write(out);
        out.close();
        return out.toByteArray();
    }

    // ==================== 兼容旧接口 ====================
    @Deprecated
    public byte[] generatePptx(String title, String contentHtml) throws Exception {
        PptxOutline outline = new PptxOutline();
        outline.setTitle(title);
        outline.setSubtitle("");
        List<PptxOutline.Slide> slides = new ArrayList<>();
        PptxOutline.Slide cover = new PptxOutline.Slide();
        cover.setType("cover");
        cover.setHeading(title);
        slides.add(cover);
        PptxOutline.Slide content = new PptxOutline.Slide();
        content.setType("content");
        content.setHeading("课程内容");
        content.setPoints(java.util.Collections.singletonList("请使用方案B结构化大纲生成完整PPT"));
        slides.add(content);
        outline.setSlides(slides);
        return generateFromOutline(outline);
    }
}
