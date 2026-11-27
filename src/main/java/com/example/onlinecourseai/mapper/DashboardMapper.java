package com.example.onlinecourseai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 教师首页看板聚合 Mapper（按当前教师统计）
 */
@Mapper
public interface DashboardMapper {

    /** 我发布的任务总数 */
    @Select("SELECT COUNT(*) FROM task WHERE teacher_id = #{teacherId}")
    int countTasks(Long teacherId);

    /** 待批阅（REVIEWING）人次 */
    @Select("SELECT COUNT(*) FROM grade g JOIN task t ON g.task_id = t.id " +
            "WHERE t.teacher_id = #{teacherId} AND g.status = 'REVIEWING'")
    int countPendingReview(Long teacherId);

    /** 已批阅（GRADED）人次 */
    @Select("SELECT COUNT(*) FROM grade g JOIN task t ON g.task_id = t.id " +
            "WHERE t.teacher_id = #{teacherId} AND g.status = 'GRADED'")
    int countGraded(Long teacherId);

    /** 已提交作业人次（grade 有记录） */
    @Select("SELECT COUNT(*) FROM grade g JOIN task t ON g.task_id = t.id " +
            "WHERE t.teacher_id = #{teacherId}")
    int countSubmitted(Long teacherId);

    /** 已批阅作业的平均总分 */
    @Select("SELECT IFNULL(ROUND(AVG(g.total_score), 1), 0) FROM grade g JOIN task t ON g.task_id = t.id " +
            "WHERE t.teacher_id = #{teacherId} AND g.status = 'GRADED'")
    double avgGradedScore(Long teacherId);

    /** 收到的学生反馈总数 */
    @Select("SELECT COUNT(*) FROM courseware_feedback f JOIN animation a ON f.animation_id = a.id " +
            "WHERE a.teacher_id = #{teacherId}")
    int countFeedback(Long teacherId);

    /** 我发布的任务中已关闭数量 */
    @Select("SELECT COUNT(*) FROM task WHERE teacher_id = #{teacherId} AND status = 'CLOSED'")
    int countClosedTasks(Long teacherId);
}
