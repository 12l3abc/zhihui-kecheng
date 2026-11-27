package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.mapper.DashboardMapper;
import com.example.onlinecourseai.mapper.TeacherNotificationMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 教师首页数据看板：任务、批阅、提交、平均分、反馈、未读通知的概览聚合
 */
@RestController
@RequestMapping("/api/teacher")
public class DashboardController {

    private final DashboardMapper dashboardMapper;
    private final TeacherNotificationMapper notifyMapper;

    public DashboardController(DashboardMapper dashboardMapper,
                                TeacherNotificationMapper notifyMapper) {
        this.dashboardMapper = dashboardMapper;
        this.notifyMapper = notifyMapper;
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"teacher".equals(role)) {
            throw new BizException("只有教师可以查看工作台");
        }
        Long teacherId = (Long) session.getAttribute("userId");

        int taskCount = dashboardMapper.countTasks(teacherId);
        int pending = dashboardMapper.countPendingReview(teacherId);
        int graded = dashboardMapper.countGraded(teacherId);
        int submitted = dashboardMapper.countSubmitted(teacherId);
        double avgScore = dashboardMapper.avgGradedScore(teacherId);
        int feedback = dashboardMapper.countFeedback(teacherId);
        int closed = dashboardMapper.countClosedTasks(teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("taskCount", taskCount);
        data.put("activeTasks", taskCount - closed);
        data.put("pendingReview", pending);
        data.put("gradedCount", graded);
        data.put("submittedCount", submitted);
        data.put("avgScore", avgScore);
        data.put("feedbackCount", feedback);
        data.put("unreadNotify", notifyMapper.countUnread(teacherId));
        return Result.success(data);
    }
}
