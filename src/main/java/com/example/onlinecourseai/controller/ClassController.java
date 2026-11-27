package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.StudentVO;
import com.example.onlinecourseai.entity.ClassInfo;
import com.example.onlinecourseai.service.ClassService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassController {

    private final ClassService classService;

    public ClassController(ClassService classService) {
        this.classService = classService;
    }

    /** 教师创建班级 */
    @PostMapping
    public Result<ClassInfo> create(@RequestBody Map<String, String> body, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以创建班级");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.createClass(userId, body.get("className")));
    }

    /** 我的班级（教师：我创建的；学生：我加入的） */
    @GetMapping("/my")
    public Result<List<ClassInfo>> my(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        return Result.success(classService.getMyClasses(userId, role));
    }

    /** 学生加入班级 */
    @PostMapping("/{id}/join")
    public Result<ClassInfo> join(@PathVariable Long id, HttpSession session) {
        checkRole(session, "student", "只有学生可以加入班级");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.joinClass(userId, id));
    }

    /** 教师查看班级学生名单（只能看自己创建的班级） */
    @GetMapping("/{id}/students")
    public Result<List<StudentVO>> students(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以查看学生名单");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.getClassStudents(userId, id));
    }

    /** 教师把已注册学生添加进班级（按学号/用户名） */
    @PostMapping("/{id}/students")
    public Result<ClassInfo> addStudent(@PathVariable Long id,
                                        @RequestBody Map<String, String> body,
                                        HttpSession session) {
        checkRole(session, "teacher", "只有教师可以添加学生");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.addStudent(userId, id, body.get("username")));
    }

    /** 教师把学生转出班级 */
    @DeleteMapping("/{id}/students/{studentId}")
    public Result<Void> removeStudent(@PathVariable Long id,
                                      @PathVariable Long studentId,
                                      HttpSession session) {
        checkRole(session, "teacher", "只有教师可以转出学生");
        Long userId = (Long) session.getAttribute("userId");
        classService.removeStudent(userId, id, studentId);
        return Result.success();
    }

    /** 教师把学生转入另一班级（转班） */
    @PostMapping("/{id}/students/{studentId}/move")
    public Result<Void> moveStudent(@PathVariable Long id,
                                    @PathVariable Long studentId,
                                    @RequestBody Map<String, Object> body,
                                    HttpSession session) {
        checkRole(session, "teacher", "只有教师可以调整学生班级");
        Long userId = (Long) session.getAttribute("userId");
        Long targetClassId = body.get("targetClassId") == null ? null : Long.valueOf(body.get("targetClassId").toString());
        classService.moveStudent(userId, id, studentId, targetClassId);
        return Result.success();
    }

    /** 教师添加学生：按学号/姓名/专业搜索已注册学生 */
    @GetMapping("/students/search")
    public Result<java.util.List<com.example.onlinecourseai.entity.SysUser>> searchStudents(@RequestParam(required = false) String keyword,
                                                                                            HttpSession session) {
        checkRole(session, "teacher", "只有教师可以搜索学生");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.searchStudents(userId, keyword));
    }

    /** 教师修改班级名称（同名查重） */
    @PostMapping("/{id}/update")
    public Result<ClassInfo> update(@PathVariable Long id,
                                    @RequestBody Map<String, String> body,
                                    HttpSession session) {
        checkRole(session, "teacher", "只有教师可以修改班级");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.updateClass(userId, id, body.get("className")));
    }

    /** 教师删除班级（有学生或任务时拒绝） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以删除班级");
        Long userId = (Long) session.getAttribute("userId");
        classService.deleteClass(userId, id);
        return Result.success();
    }

    /** 教师按名称搜索自己的班级 */
    @GetMapping("/search")
    public Result<List<ClassInfo>> search(@RequestParam(required = false) String keyword,
                                          HttpSession session) {
        checkRole(session, "teacher", "只有教师可以搜索班级");
        Long userId = (Long) session.getAttribute("userId");
        return Result.success(classService.searchClasses(userId, keyword));
    }

    /** 班级合并：源班级的学生与任务并入目标班级，源班级删除 */
    @PostMapping("/merge")
    public Result<List<ClassInfo>> merge(@RequestBody Map<String, Object> body, HttpSession session) {
        checkRole(session, "teacher", "只有教师可以合并班级");
        Long userId = (Long) session.getAttribute("userId");
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.getOrDefault("sourceIds", new ArrayList<Object>());
        List<Long> sourceIds = new ArrayList<>();
        for (Object o : rawIds) {
            sourceIds.add(Long.valueOf(o.toString()));
        }
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(body.get("targetId").toString());
        return Result.success(classService.mergeClasses(userId, sourceIds, targetId));
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }
}
