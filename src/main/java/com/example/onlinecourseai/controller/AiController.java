package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.CoursewareVO;
import com.example.onlinecourseai.dto.PptxOutline;
import com.example.onlinecourseai.dto.QuestionVO;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.service.AiCoursewareService;
import com.example.onlinecourseai.service.PptxGeneratorService;
import com.example.onlinecourseai.service.PptxOutlineService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * AI 课件生成与查看接口
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiCoursewareService aiCoursewareService;
    private final PptxGeneratorService pptxGeneratorService;
    private final PptxOutlineService pptxOutlineService;

    public AiController(AiCoursewareService aiCoursewareService,
                        PptxGeneratorService pptxGeneratorService,
                        PptxOutlineService pptxOutlineService) {
        this.aiCoursewareService = aiCoursewareService;
        this.pptxGeneratorService = pptxGeneratorService;
        this.pptxOutlineService = pptxOutlineService;
    }

    /** 教师根据知识点生成动画课件 */
    @PostMapping("/generate")
    public Result<CoursewareVO> generate(@RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以生成课件");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(aiCoursewareService.generateCourseware(userId, body.get("knowledgePoint")));
    }

    /** 我的课件列表（教师生成的课件） */
    @GetMapping("/my")
    public Result<List<CoursewareVO>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(aiCoursewareService.getMyCoursewares(userId));
    }

    /** 课件详情（含完整 HTML 和配套题目）；教师限本人课件，学生限班级任务引用 */
    @GetMapping("/{id}")
    public Result<CoursewareVO> detail(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        CoursewareVO vo = aiCoursewareService.getCoursewareDetailVisible(id, userId, role);
        // 学生浏览课件时不返回答案，避免提前看到客观题正确项/主观题参考范文
        if (!"teacher".equals(role) && vo.getQuestions() != null) {
            for (QuestionVO q : vo.getQuestions()) {
                q.setAnswer(null);
            }
        }
        return Result.success(vo);
    }

    /** 教师对课件生成重点知识提炼（更新 summary，学生端展示） */
    @PostMapping("/{id}/summary")
    public Result<String> summary(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以生成重点提炼");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(aiCoursewareService.generateSummary(userId, id));
    }

    /** 教师更新课件标题、摘要；可选整块回写课件正文 HTML（编辑模式改文字后保存） */
    @PostMapping("/{id}/update")
    public Result<Animation> update(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    HttpSession session) {
        checkRole(session, "teacher", "只有教师可以修改课件");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(aiCoursewareService.updateCourseware(
                userId, id, body.get("title"), body.get("summary"), body.get("contentHtml")));
    }

    /** 教师修改客观题或主观题参考答案（question 表为唯一数据源） */
    @PostMapping("/{id}/quiz-answer")
    public Result<Boolean> quizAnswer(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      HttpSession session) {
        checkRole(session, "teacher", "只有教师可以修改题目答案");
        Long userId = (Long) session.getAttribute("userId");
        Long questionId = parseLong(body.get("questionId"));
        String questionText = body.get("question") == null ? null : body.get("question").toString();
        String answer = body.get("answer") == null ? null : body.get("answer").toString();
        return Result.success(aiCoursewareService.updateQuizAnswer(
                userId, id, questionId, questionText, answer));
    }

    /** 教师删除课件（已被任务引用时拒绝） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以删除课件");
        Long userId = (Long) session.getAttribute("userId");
        aiCoursewareService.deleteCourseware(userId, id);
        return Result.success();
    }

    /** 下载课件为 PPTX 文件（方案B：大模型结构化解析 → 生成PPTX） */
    @GetMapping("/{id}/download-pptx")
    public void downloadPptx(@PathVariable Long id, HttpSession session, HttpServletResponse response) throws Exception {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        CoursewareVO cw = aiCoursewareService.getCoursewareDetailVisible(id, userId, role);
        if (cw == null || cw.getContentHtml() == null) {
            response.setStatus(404);
            return;
        }
        // 方案B：大模型结构化解析（带缓存），再生成PPTX
        PptxOutline outline = pptxOutlineService.getOutline(id, cw.getTitle(), cw.getSummary(), cw.getContentHtml());
        byte[] pptxBytes = pptxGeneratorService.generateFromOutline(outline);
        String fileName = URLEncoder.encode(cw.getTitle() + ".pptx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
        response.setContentLength(pptxBytes.length);
        response.getOutputStream().write(pptxBytes);
        response.getOutputStream().flush();
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }

    private Long parseLong(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new BizException("题目 ID 格式错误");
        }
    }
}
