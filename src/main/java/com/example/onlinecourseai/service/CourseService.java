package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.CourseAnalysisResult;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.CourseInfo;

import java.util.List;
import java.util.Map;

public interface CourseService {

    CourseInfo createCourse(Long teacherId, Map<String, Object> body);

    List<CourseInfo> listMyCourses(Long teacherId);

    CourseInfo updateCourse(Long teacherId, Long courseId, Map<String, Object> body);

    void deleteCourse(Long teacherId, Long courseId);

    List<CourseInfo> searchCourses(Long teacherId, String keyword);

    /**
     * 智能归科：分析教师全部课件所属科目，自动建立（或复用）对应课程并回写课件归属
     */
    CourseAnalysisResult analyzeAndAssign(Long teacherId);

    /** 某门课程下的课件清单（不含 content_html） */
    List<Animation> listCoursewares(Long teacherId, Long courseId);

    /**
     * 调整课件归属：把课件移动到目标课程；courseId 为空则移出课程（未归科）
     * 校验课件与目标课程均属于当前教师
     */
    void moveCourseware(Long teacherId, Long animationId, Long courseId);
}
