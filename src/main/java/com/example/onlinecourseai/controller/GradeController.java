package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.MyGradeVO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.service.GradeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成绩排名接口
 */
@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    /** 学生查看自己的成绩列表（含班级排名） */
    @GetMapping("/my")
    public Result<List<MyGradeVO>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(gradeService.getMyGrades(userId));
    }

    /** 教师查看某任务的班级成绩单（按总分排名） */
    @GetMapping("/task/{taskId}")
    public Result<List<ReviewStudentVO>> ranking(@PathVariable Long taskId, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看成绩单");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(gradeService.getTaskRanking(userId, taskId));
    }

    /** 教师查看某任务的成绩分析（分数段+平均分+提交率） */
    @GetMapping("/task/{taskId}/analysis")
    public Result<java.util.Map<String, Object>> analysis(@PathVariable Long taskId, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看成绩分析");
        Long userId = (Long) session.getAttribute("userId");
        // 校验任务属于当前教师
        com.example.onlinecourseai.entity.Task task = gradeService.getTaskEntity(taskId);
        com.example.onlinecourseai.entity.ClassInfo clazz = gradeService.getClassInfo(task.getClassId());
        if (!clazz.getTeacherId().equals(userId)) {
            throw new BizException("只能查看自己发布任务的成绩分析");
        }
        java.util.Map<String, Object> dist = gradeService.getScoreDistribution(taskId);
        int totalStudents = gradeService.getClassStudentCount(taskId);
        int submitted = gradeService.getSubmittedCount(taskId);
        dist.put("totalStudents", totalStudents);
        dist.put("submitted", submitted);
        dist.put("unsubmitted", totalStudents - submitted);
        return Result.success(dist);
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }
}
