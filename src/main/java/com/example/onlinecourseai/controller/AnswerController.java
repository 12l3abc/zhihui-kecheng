package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.AnswerItemDTO;
import com.example.onlinecourseai.dto.AnswerSubmitDTO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.dto.StudentAnswerVO;
import com.example.onlinecourseai.dto.StudentTaskResultVO;
import com.example.onlinecourseai.dto.SubmitResultVO;
import com.example.onlinecourseai.entity.Grade;
import com.example.onlinecourseai.service.AnswerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 问答模块接口（作答提交 + 教师批阅）
 */
@RestController
@RequestMapping("/api/tasks")
public class AnswerController {

    private final AnswerService answerService;

    public AnswerController(AnswerService answerService) {
        this.answerService = answerService;
    }

    /** 学生提交任务作答 */
    @PostMapping("/{taskId}/submit")
    public Result<SubmitResultVO> submit(@PathVariable Long taskId,
                                         @RequestBody AnswerSubmitDTO body,
                                         HttpSession session) {
        checkRole(session, "student", "只有学生可以提交作答");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.submitAnswers(userId, taskId, body.getAnswers()));
    }

    /** 学生查看自己某次任务的作答与参考答案（仅提交后可见） */
    @GetMapping("/{taskId}/my-result")
    public Result<StudentTaskResultVO> myResult(@PathVariable Long taskId, HttpSession session) {
        checkRole(session, "student", "只有学生可以查看自己的作答");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.getMyResult(userId, taskId));
    }

    /** 教师查看任务批阅总览 */
    @GetMapping("/{taskId}/review")
    public Result<List<ReviewStudentVO>> review(@PathVariable Long taskId, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看批阅总览");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.getReviewOverview(userId, taskId));
    }

    /** 教师查看某学生逐题作答 */
    @GetMapping("/{taskId}/review/{studentId}")
    public Result<List<StudentAnswerVO>> studentAnswers(@PathVariable Long taskId,
                                                        @PathVariable Long studentId,
                                                        HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看作答详情");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.getStudentAnswers(userId, taskId, studentId));
    }

    /** 教师批阅主观题：填写得分与评语；可附带修改后的逐题答案（客观重判、主观回写） */
    @PostMapping("/{taskId}/grade")
    @SuppressWarnings("unchecked")
    public Result<Grade> grade(@PathVariable Long taskId,
                               @RequestBody Map<String, Object> body,
                               HttpSession session) {
        checkRole(session, "teacher", "只有教师可以批阅");
        Long userId = (Long) session.getAttribute("userId");
        Long studentId = Long.valueOf(body.get("studentId").toString());
        Integer teacherScore = Integer.valueOf(body.get("teacherScore").toString());
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        List<AnswerItemDTO> answers = null;
        Object rawAnswers = body.get("answers");
        if (rawAnswers instanceof List) {
            answers = new java.util.ArrayList<>();
            for (Object o : (List<Object>) rawAnswers) {
                Map<String, Object> m = (Map<String, Object>) o;
                AnswerItemDTO it = new AnswerItemDTO();
                it.setQuestionId(m.get("questionId") == null ? null : Long.valueOf(m.get("questionId").toString()));
                it.setAnswerText(m.get("answerText") == null ? null : m.get("answerText").toString());
                answers.add(it);
            }
        }
        return Result.success(answerService.gradeStudent(userId, taskId, studentId, teacherScore, comment, answers));
    }

    /** 教师打回重做：清空该生作答与成绩；传入 extendDeadline 时为其单独顺延补交截止 */
    @PostMapping("/{taskId}/reject")
    public Result<Void> reject(@PathVariable Long taskId,
                               @RequestBody Map<String, Object> body,
                               HttpSession session) {
        checkRole(session, "teacher", "只有教师可以打回作业");
        Long userId = (Long) session.getAttribute("userId");
        Long studentId = Long.valueOf(body.get("studentId").toString());
        LocalDateTime extendDeadline = null;
        String extRaw = body.get("extendDeadline") == null ? null : body.get("extendDeadline").toString();
        if (extRaw != null && !extRaw.isBlank()) {
            try {
                extendDeadline = LocalDateTime.parse(extRaw);
            } catch (Exception e) {
                throw new BizException("补交截止时间格式错误，应为 yyyy-MM-ddTHH:mm");
            }
        }
        answerService.rejectStudent(userId, taskId, studentId, extendDeadline);
        return Result.success(null);
    }

    /** 教师为未提交学生单独顺延补交截止（不清空作答，学生在新窗口内解锁提交） */
    @PostMapping("/{taskId}/extend")
    public Result<Void> extend(@PathVariable Long taskId,
                               @RequestBody Map<String, Object> body,
                               HttpSession session) {
        checkRole(session, "teacher", "只有教师可以顺延补交");
        Long userId = (Long) session.getAttribute("userId");
        Long studentId = Long.valueOf(body.get("studentId").toString());
        LocalDateTime extendDeadline = null;
        String extRaw = body.get("extendDeadline") == null ? null : body.get("extendDeadline").toString();
        if (extRaw != null && !extRaw.isBlank()) {
            try {
                extendDeadline = LocalDateTime.parse(extRaw);
            } catch (Exception e) {
                throw new BizException("补交截止时间格式错误，应为 yyyy-MM-ddTHH:mm");
            }
        }
        answerService.extendDeadline(userId, taskId, studentId, extendDeadline);
        return Result.success(null);
    }

    /** 教师催交：给未提交学生写一条催交提醒 */
    @PostMapping("/{taskId}/remind")
    public Result<Void> remind(@PathVariable Long taskId,
                              @RequestBody Map<String, Object> body,
                              HttpSession session) {
        checkRole(session, "teacher", "只有教师可以催交");
        Long userId = (Long) session.getAttribute("userId");
        Long studentId = Long.valueOf(body.get("studentId").toString());
        answerService.remindStudent(userId, taskId, studentId);
        return Result.success(null);
    }

    /** 学生查询本任务是否有未读催交，并标记已读 */
    @GetMapping("/{taskId}/remind")
    public Result<Map<String, Object>> remindStatus(@PathVariable Long taskId, HttpSession session) {
        checkRole(session, "student", "只有学生可以查看催交");
        Long userId = (Long) session.getAttribute("userId");
        boolean unread = answerService.hasUnreadRemind(userId, taskId);
        answerService.readReminds(userId, taskId);
        return Result.success(Map.of("hasUnread", unread));
    }

    /** 学生查询自己所有未读催交涉及的任务 id（不标已读，供任务列表红点） */
    @GetMapping("/my/remind-list")
    public Result<List<Long>> myRemindList(HttpSession session) {
        checkRole(session, "student", "只有学生可以查看催交");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.listUnreadRemindTaskIds(userId));
    }

    /** 学生查看自己收到的催交通知明细（含已读，供通知栏目） */
    @GetMapping("/my/notices")
    public Result<List<Map<String, Object>>> myNotices(HttpSession session) {
        checkRole(session, "student", "只有学生可以查看通知");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(answerService.listMyNotices(userId));
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }
}
