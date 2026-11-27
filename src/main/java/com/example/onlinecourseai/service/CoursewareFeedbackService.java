package com.example.onlinecourseai.service;

import com.example.onlinecourseai.entity.CoursewareFeedback;

import java.util.List;

/**
 * 课件评价与疑问服务
 */
public interface CoursewareFeedbackService {

    /** 学生提交评价或疑问 */
    CoursewareFeedback submitFeedback(Long studentId, String studentName, Long animationId, String type, String content);

    /** 查询某个课件的评价/疑问（教师只能看自己的课件，学生只能看本班任务可访问的课件） */
    List<CoursewareFeedback> getByAnimationId(Long animationId, Long viewerId, String role);

    /** 学生查询自己提交的所有评价/疑问 */
    List<CoursewareFeedback> getByStudentId(Long studentId);

    /** 教师查询自己所有课件的评价/疑问 */
    List<CoursewareFeedback> getByTeacherId(Long teacherId);

    /** 学生修改自己提交的反馈内容（校验归属） */
    CoursewareFeedback updateOwnFeedback(Long id, Long studentId, String content);

    /** 学生删除自己提交的反馈（校验归属） */
    void deleteOwnFeedback(Long id, Long studentId);
}
