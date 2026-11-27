package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.StudyStatusVO;
import com.example.onlinecourseai.dto.TaskDetailVO;
import com.example.onlinecourseai.dto.TaskVO;
import com.example.onlinecourseai.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 教学任务接口
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 教师把课件发布为任务到指定班级 */
    @PostMapping
    public Result<TaskVO> publish(@RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以发布任务");
        Long userId = (Long) session.getAttribute("userId");
        Long classId = parseLong(body.get("classId"));
        Long animationId = parseLong(body.get("animationId"));
        LocalDateTime deadline = parseDeadline(body.get("deadline"));
        return Result.success(taskService.publishTask(userId, body.get("title"), classId, animationId, deadline));
    }

    /** 教师修改任务（标题/截止时间/班级） */
    @PutMapping("/{id}")
    public Result<TaskVO> update(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以修改任务");
        Long userId = (Long) session.getAttribute("userId");
        LocalDateTime deadline = parseDeadline(body.get("deadline"));
        Long classId = parseLong(body.get("classId"));
        return Result.success(taskService.updateTask(userId, id, body.get("title"), classId, deadline));
    }

    /** 教师删除任务 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以删除任务");
        Long userId = (Long) session.getAttribute("userId");
        taskService.deleteTask(userId, id);
        return Result.success(null);
    }

    /** 教师切换任务状态（开启/关闭） */
    @PutMapping("/{id}/toggle")
    public Result<TaskVO> toggle(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以操作任务状态");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(taskService.toggleTaskStatus(userId, id));
    }

    /** 教师搜索任务（按标题关键词、按班级筛选） */
    @GetMapping("/search")
    public Result<List<TaskVO>> search(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long classId,
                                      HttpSession session) {
        checkRole(session, "teacher", "只有教师可以搜索任务");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(taskService.searchTasks(userId, keyword, classId));
    }

    /** 我的任务（教师：我发布的；学生：我所在班级收到的） */
    @GetMapping("/my")
    public Result<List<TaskVO>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        return Result.success(taskService.getMyTasks(userId, role));
    }

    /** 学生打开任务课件，开始累计学习时长 */
    @PostMapping("/{id}/study-start")
    public Result<StudyStatusVO> studyStart(@PathVariable Long id, HttpSession session) {
        checkRole(session, "student", "只有学生可以记录课件学习");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(taskService.startStudy(userId, id));
    }

    /** 课件页学习心跳，仅在学生停留在课件页时累加有效学习秒数 */
    @PostMapping("/{id}/study-heartbeat")
    public Result<StudyStatusVO> studyHeartbeat(@PathVariable Long id, HttpSession session) {
        checkRole(session, "student", "只有学生可以记录课件学习");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(taskService.heartbeatStudy(userId, id));
    }

    /** 学生确认学完课件（服务端校验最少停留时长） */
    @PostMapping("/{id}/study-confirm")
    public Result<StudyStatusVO> studyConfirm(@PathVariable Long id, HttpSession session) {
        checkRole(session, "student", "只有学生可以确认课件学习");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(taskService.confirmStudy(userId, id));
    }

    /** 任务详情（含课件动画与题目） */
    @GetMapping("/{id}")
    public Result<TaskDetailVO> detail(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        return Result.success(taskService.getTaskDetail(userId, role, id));
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

    /** 截止时间可空，格式如 2026-09-20T23:59 */
    private LocalDateTime parseDeadline(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            throw new BizException("截止时间格式错误，应为 yyyy-MM-ddTHH:mm");
        }
    }
}
