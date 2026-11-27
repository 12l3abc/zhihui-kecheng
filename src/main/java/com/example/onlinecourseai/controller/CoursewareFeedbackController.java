package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.entity.CoursewareFeedback;
import com.example.onlinecourseai.service.CoursewareFeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 课件评价与疑问接口
 */
@RestController
@RequestMapping("/api/feedback")
public class CoursewareFeedbackController {

    private final CoursewareFeedbackService feedbackService;

    public CoursewareFeedbackController(CoursewareFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 学生提交评价或疑问 */
    @PostMapping
    public Result<CoursewareFeedback> submit(@RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "student", "只有学生可以提交评价和疑问");
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        Long animationId = parseLong(body.get("animationId"));
        String type = body.get("type");
        String content = body.get("content");
        return Result.success(feedbackService.submitFeedback(userId, username, animationId, type, content));
    }

    /** 查询某个课件的评价/疑问（学生/教师按可见范围过滤） */
    @GetMapping("/courseware/{animationId}")
    public Result<List<CoursewareFeedback>> byCourseware(@PathVariable("animationId") Long animationId,
                                                         HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            throw new BizException("请先登录");
        }
        return Result.success(feedbackService.getByAnimationId(animationId, userId, role));
    }

    /** 学生查看自己提交的所有评价/疑问 */
    @GetMapping("/my")
    public Result<List<CoursewareFeedback>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(feedbackService.getByStudentId(userId));
    }

    /** 教师查看自己所有课件的评价/疑问 */
    @GetMapping("/teacher")
    public Result<List<CoursewareFeedback>> teacher(HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看学生反馈");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(feedbackService.getByTeacherId(userId));
    }

    /** 学生修改自己提交的反馈内容 */
    @PutMapping("/{id}")
    public Result<CoursewareFeedback> update(@PathVariable("id") Long id, @RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "student", "只有学生可以修改自己的反馈");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(feedbackService.updateOwnFeedback(id, userId, body.get("content")));
    }

    /** 学生删除自己提交的反馈 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id, HttpSession session) {
        checkRole(session, "student", "只有学生可以删除自己的反馈");
        Long userId = (Long) session.getAttribute("userId");
        feedbackService.deleteOwnFeedback(id, userId);
        return Result.success();
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BizException("参数格式错误：" + value);
        }
    }
}
