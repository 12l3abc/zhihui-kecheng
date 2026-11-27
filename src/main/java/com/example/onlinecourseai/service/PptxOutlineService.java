package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.PptxOutline;
import com.example.onlinecourseai.service.impl.MockLlmService;
import com.example.onlinecourseai.service.impl.RemoteLlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 方案B：大模型结构化解析服务
 * 将课件 HTML 内容通过大模型理解为统一的 PPT 大纲 JSON，
 * 支持时间轴/概念/步骤/对比/习题等多种课件类型。
 */
@Service
public class PptxOutlineService {

    private final RemoteLlmService remoteLlmService;
    private final MockLlmService mockLlmService;
    private final ObjectMapper objectMapper;

    @Value("${llm.api-key:}")
    private String apiKey;

    /** 缓存：课件ID -> 结构化大纲，避免重复调用大模型 */
    private final ConcurrentHashMap<Long, PptxOutline> cache = new ConcurrentHashMap<>();

    public PptxOutlineService(RemoteLlmService remoteLlmService,
                              MockLlmService mockLlmService,
                              ObjectMapper objectMapper) {
        this.remoteLlmService = remoteLlmService;
        this.mockLlmService = mockLlmService;
        this.objectMapper = objectMapper;
    }

    /** 有 Key 用豆包，无 Key 用 Mock */
    private LlmService pickLlm() {
        if (apiKey != null && !apiKey.isBlank()) {
            return remoteLlmService;
        }
        return mockLlmService;
    }

    /**
     * 获取课件的结构化大纲（带缓存）
     */
    public PptxOutline getOutline(Long coursewareId, String title, String summary, String contentHtml) {
        if (coursewareId != null && cache.containsKey(coursewareId)) {
            return cache.get(coursewareId);
        }
        PptxOutline outline = parseToOutline(title, summary, contentHtml);
        if (coursewareId != null) {
            cache.put(coursewareId, outline);
        }
        return outline;
    }

    /**
     * 调用大模型将课件内容解析为结构化大纲
     */
    private PptxOutline parseToOutline(String title, String summary, String contentHtml) {
        String plainText = stripHtml(contentHtml);
        // 限制文本长度，避免超出大模型上下文
        if (plainText.length() > 3000) {
            plainText = plainText.substring(0, 3000) + "...";
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = "请解析以下教学课件内容，输出结构化PPT大纲JSON。\n\n"
                + "课件标题：" + title + "\n"
                + "课件摘要：" + (summary != null ? summary : "") + "\n\n"
                + "课件正文：\n" + plainText + "\n\n"
                + "请严格输出JSON，不要输出任何其他文字。";

        String jsonStr = pickLlm().chat(systemPrompt, userPrompt);
        // 清理可能的markdown代码块标记
        jsonStr = jsonStr.trim();
        if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
        }

        try {
            return objectMapper.readValue(jsonStr, PptxOutline.class);
        } catch (Exception e) {
            // 解析失败时返回兜底大纲
            return buildFallbackOutline(title, summary, plainText);
        }
    }

    private String buildSystemPrompt() {
        return "你是一个专业的教学课件结构化解析助手。你的任务是将教学课件内容解析为统一的PPT大纲JSON格式。\n\n"
                + "【输出格式要求】\n"
                + "严格输出以下JSON结构，不要输出任何解释文字：\n"
                + "{\n"
                + "  \"title\": \"课件标题\",\n"
                + "  \"subtitle\": \"副标题/摘要\",\n"
                + "  \"slides\": [\n"
                + "    {\"type\": \"cover\", \"heading\": \"主标题\", \"subheading\": \"副标题\"},\n"
                + "    {\"type\": \"content\", \"heading\": \"章节标题\", \"points\": [\"要点1\", \"要点2\"], \"interactive\": true},\n"
                + "    {\"type\": \"timeline\", \"heading\": \"发展历程\", \"events\": [{\"year\": \"1921年\", \"title\": \"事件名\", \"desc\": \"描述\"}], \"interactive\": true},\n"
                + "    {\"type\": \"steps\", \"heading\": \"操作步骤\", \"steps\": [{\"number\": \"1\", \"title\": \"步骤名\", \"desc\": \"描述\"}], \"interactive\": true},\n"
                + "    {\"type\": \"comparison\", \"heading\": \"对比分析\", \"comparison\": [{\"left\": \"A特点\", \"right\": \"B特点\"}], \"interactive\": true},\n"
                + "    {\"type\": \"quiz\", \"heading\": \"随堂练习\", \"quiz\": {\"question\": \"题目\", \"options\": [\"A.选项\", \"B.选项\", \"C.选项\", \"D.选项\"], \"answer\": \"B\", \"explanation\": \"解析\"}}\n"
                + "  ]\n"
                + "}\n\n"
                + "【幻灯片类型说明】\n"
                + "- cover：封面页，只有标题和副标题\n"
                + "- content：内容讲解页，标题+要点列表，interactive=true表示要点点击逐步显示\n"
                + "- timeline：时间轴页，所有事件放在同一页，interactive=true表示事件点击逐步显示\n"
                + "- steps：步骤演示页，所有步骤放在同一页，interactive=true表示步骤点击逐步显示\n"
                + "- comparison：对比分析页，左右对比，interactive=true表示对比项点击逐步显示\n"
                + "- quiz：随堂练习页，题目+选项+答案\n\n"
                + "【重要原则】\n"
                + "1. 第一页必须是cover类型\n"
                + "2. 时间轴、步骤、对比类型的内容必须放在同一页（一页展示全部），通过interactive实现点击逐步显示\n"
                + "3. 内容讲解页如果要点较多，可以分成多页，但每页要点不超过5条\n"
                + "4. 最后一页通常是quiz类型（如果有习题）\n"
                + "5. 忠实于原文内容，不要编造不存在的知识点\n"
                + "6. 每个事件/步骤/要点的描述简洁明了，不超过80字";
    }

    /**
     * 解析失败时的兜底大纲（直接用文本内容生成）
     */
    private PptxOutline buildFallbackOutline(String title, String summary, String plainText) {
        PptxOutline outline = new PptxOutline();
        outline.setTitle(title != null ? title : "教学课件");
        outline.setSubtitle(summary != null ? summary : "");

        java.util.List<PptxOutline.Slide> slides = new java.util.ArrayList<>();

        // 封面
        PptxOutline.Slide cover = new PptxOutline.Slide();
        cover.setType("cover");
        cover.setHeading(title != null ? title : "教学课件");
        cover.setSubheading(summary != null ? summary : "");
        slides.add(cover);

        // 内容页（简单分段）
        PptxOutline.Slide content = new PptxOutline.Slide();
        content.setType("content");
        content.setHeading("课程内容");
        content.setInteractive(true);
        String[] paragraphs = plainText.split("\\n+");
        java.util.List<String> points = new java.util.ArrayList<>();
        for (String p : paragraphs) {
            p = p.trim();
            if (!p.isEmpty() && p.length() > 5) {
                points.add(p.length() > 100 ? p.substring(0, 100) + "..." : p);
                if (points.size() >= 5) break;
            }
        }
        content.setPoints(points);
        slides.add(content);

        outline.setSlides(slides);
        return outline;
    }

    /**
     * 去除HTML标签，提取纯文本
     */
    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) return "";
        // 移除script和style标签及内容
        html = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        html = html.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        // 移除所有HTML标签
        html = html.replaceAll("(?is)<[^>]+>", " ");
        // 移除HTML实体
        html = html.replaceAll("&nbsp;", " ");
        html = html.replaceAll("&amp;", "&");
        html = html.replaceAll("&lt;", "<");
        html = html.replaceAll("&gt;", ">");
        html = html.replaceAll("&quot;", "\"");
        // 合并多余空白
        html = html.replaceAll("\\s+", " ");
        return html.trim();
    }
}
