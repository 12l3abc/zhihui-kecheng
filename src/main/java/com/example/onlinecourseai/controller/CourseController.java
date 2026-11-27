package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.CourseAnalysisResult;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.CourseInfo;
import com.example.onlinecourseai.service.CourseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public Result<CourseInfo> create(@RequestBody Map<String, Object> body, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.createCourse(userId, body));
    }

    @GetMapping("/my")
    public Result<List<CourseInfo>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.listMyCourses(userId));
    }

    @PostMapping("/{id}/update")
    public Result<CourseInfo> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.updateCourse(userId, id, body));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        courseService.deleteCourse(userId, id);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<CourseInfo>> search(@RequestParam(required = false) String keyword, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.searchCourses(userId, keyword));
    }

    /** 智能归科：分析我的全部课件所属科目，自动建课并回写归属 */
    @PostMapping("/analyze")
    public Result<CourseAnalysisResult> analyze(HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.analyzeAndAssign(userId));
    }

    /** 某门课程下的课件清单 */
    @GetMapping("/{id}/coursewares")
    public Result<List<Animation>> coursewares(@PathVariable Long id, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(courseService.listCoursewares(userId, id));
    }

    /** 调整课件归属：移动到指定课程；courseId 为空则移出课程（未归科） */
    @PutMapping("/move")
    public Result<Void> move(@RequestBody Map<String, Object> body, HttpSession session) {
        checkTeacher(session);
        Long userId = (Long) session.getAttribute("userId");
        Object cwRaw = body.get("coursewareId");
        if (cwRaw == null) throw new BizException("缺少课件标识");
        Long animationId = Long.valueOf(cwRaw.toString());
        Object raw = body.get("courseId");
        Long courseId = (raw == null || raw.toString().isEmpty()) ? null : Long.valueOf(raw.toString());
        courseService.moveCourseware(userId, animationId, courseId);
        return Result.success();
    }

    private void checkTeacher(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (!"teacher".equals(role)) throw new BizException("只有教师可以管理课程");
    }
}
