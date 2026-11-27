package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.dto.CoursewareGroupVO;
import com.example.onlinecourseai.dto.TaskVO;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.service.CourseSubjectClassifier;
import com.example.onlinecourseai.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生端课件目录接口：把学生可见课件按学科智能归类后分组返回。
 *
 * 可见范围：学生所在班级收到的任务所引用的课件；
 * 归类方式：CourseSubjectClassifier 根据课件标题、知识点、摘要加权打分判定学科。
 */
@RestController
@RequestMapping("/api/student")
public class StudentCoursewareController {

    private final TaskService taskService;
    private final AnimationMapper animationMapper;
    private final CourseSubjectClassifier classifier;

    public StudentCoursewareController(TaskService taskService,
                                        AnimationMapper animationMapper,
                                        CourseSubjectClassifier classifier) {
        this.taskService = taskService;
        this.animationMapper = animationMapper;
        this.classifier = classifier;
    }

    /** 学生课件目录：按学科分组，组内为该学科课件 */
    @GetMapping("/courseware-groups")
    public Result<List<CoursewareGroupVO>> coursewareGroups(HttpSession session) {
        checkRole(session, "student", "仅学生可查看课件目录");
        Long userId = (Long) session.getAttribute("userId");

        List<TaskVO> tasks = taskService.getMyTasks(userId, "student");

        // 按 animationId 去重，记录首个引用任务的班级名
        Map<Long, String> classNameByAnim = new LinkedHashMap<>();
        List<Long> animIds = new ArrayList<>();
        for (TaskVO t : tasks) {
            Long aid = t.getAnimationId();
            if (aid == null || classNameByAnim.containsKey(aid)) {
                continue;
            }
            classNameByAnim.put(aid, t.getClassName());
            animIds.add(aid);
        }

        // 逐课件取素材并按学科归类
        Map<String, CoursewareGroupVO> grouped = new LinkedHashMap<>();
        for (Long aid : animIds) {
            Animation a = animationMapper.findById(aid);
            if (a == null) {
                continue;
            }
            CourseSubjectClassifier.Result hit =
                    classifier.classify(a.getTitle(), a.getKnowledgePoint(), a.getSummary());
            String subject = hit.getSubject();

            CoursewareGroupVO group = grouped.computeIfAbsent(subject, k -> {
                CoursewareGroupVO vo = new CoursewareGroupVO();
                vo.setSubject(k);
                vo.setCoursewares(new ArrayList<>());
                return vo;
            });

            CoursewareGroupVO.CoursewareItem item = new CoursewareGroupVO.CoursewareItem();
            item.setAnimationId(a.getId());
            item.setTitle(a.getTitle());
            item.setKnowledgePoint(a.getKnowledgePoint());
            item.setSummary(a.getSummary());
            item.setClassName(classNameByAnim.get(aid));
            item.setCreatedAt(a.getCreatedAt());
            group.getCoursewares().add(item);
        }

        List<CoursewareGroupVO> result = new ArrayList<>(grouped.values());
        for (CoursewareGroupVO g : result) {
            g.setCount(g.getCoursewares().size());
        }
        return Result.success(result);
    }

    private void checkRole(HttpSession session, String expectRole, String message) {
        String role = (String) session.getAttribute("role");
        if (!expectRole.equals(role)) {
            throw new BizException(message);
        }
    }
}
