package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.entity.TeacherNotification;
import com.example.onlinecourseai.mapper.TeacherNotificationMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教师通知中心：学生提交作业、课件反馈等事件的未读提醒与已读管理
 */
@RestController
@RequestMapping("/api/notify")
public class NotifyController {

    private final TeacherNotificationMapper notifyMapper;

    public NotifyController(TeacherNotificationMapper notifyMapper) {
        this.notifyMapper = notifyMapper;
    }

    /** 教师通知列表 + 未读数 */
    @GetMapping("/my")
    public Result<Map<String, Object>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BizException("未登录");
        }
        List<TeacherNotification> list = notifyMapper.findByTeacherId(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("unread", notifyMapper.countUnread(userId));
        data.put("list", list);
        return Result.success(data);
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BizException("未登录");
        }
        notifyMapper.markRead(id, userId);
        return Result.success();
    }

    /** 全部标记已读 */
    @PutMapping("/read-all")
    public Result<Void> readAll(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new BizException("未登录");
        }
        notifyMapper.markAllRead(userId);
        return Result.success();
    }
}
