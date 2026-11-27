package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.dto.MyGradeVO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.entity.Grade;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 成绩表 Mapper
 */
@Mapper
public interface GradeMapper {

    @Insert("INSERT INTO grade(task_id, student_id, total_score, teacher_score, comment, status, submit_time) " +
            "VALUES(#{taskId}, #{studentId}, #{totalScore}, #{teacherScore}, #{comment}, #{status}, #{submitTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Grade grade);

    @Select("SELECT * FROM grade WHERE task_id = #{taskId} AND student_id = #{studentId}")
    Grade findByTaskAndStudent(Long taskId, Long studentId);

    @Update("UPDATE grade SET total_score = #{totalScore}, teacher_score = #{teacherScore}, " +
            "comment = #{comment}, status = #{status} WHERE id = #{id}")
    int update(Grade grade);

    /** 教师打回重做：删除该生成绩汇总，使其可重新提交 */
    @Delete("DELETE FROM grade WHERE task_id = #{taskId} AND student_id = #{studentId}")
    int deleteByTaskAndStudent(Long taskId, Long studentId);

    /**
     * 教师批阅总览：任务班级下每个学生的提交状态与得分。
     * 客观题得分按作答记录中已判分（is_correct 非空）的记录求和。
     */
    @Select("SELECT u.id AS student_id, u.username, u.real_name, " +
            "CASE WHEN g.id IS NULL THEN 0 ELSE 1 END AS submitted, " +
            "(SELECT IFNULL(SUM(r.score), 0) FROM answer_record r " +
            " WHERE r.task_id = #{taskId} AND r.student_id = u.id AND r.is_correct IS NOT NULL) AS objective_score, " +
            "IFNULL(g.teacher_score, 0) AS teacher_score, " +
            "IFNULL(g.total_score, 0) AS total_score, " +
            "IFNULL(g.comment, '') AS comment, " +
            "IFNULL(g.status, '') AS status, " +
            "(SELECT MAX(e.extend_deadline) FROM task_submit_extension e " +
            " WHERE e.task_id = #{taskId} AND e.student_id = u.id) AS extend_deadline " +
            "FROM enrollment e " +
            "JOIN sys_user u ON e.student_id = u.id " +
            "LEFT JOIN grade g ON g.task_id = #{taskId} AND g.student_id = u.id " +
            "WHERE e.class_id = (SELECT class_id FROM task WHERE id = #{taskId}) " +
            "ORDER BY g.total_score DESC, u.id")
    List<ReviewStudentVO> findReviewOverview(Long taskId);

    /** 学生个人成绩：已提交任务列表（含任务与班级名） */
    @Select("SELECT g.task_id, t.title AS task_title, c.class_name, g.total_score, g.teacher_score, " +
            "g.comment, g.status, g.submit_time " +
            "FROM grade g " +
            "JOIN task t ON g.task_id = t.id " +
            "JOIN class_info c ON t.class_id = c.id " +
            "WHERE g.student_id = #{studentId} " +
            "ORDER BY g.submit_time DESC")
    List<MyGradeVO> findMyGrades(Long studentId);

    /** 某学生在某任务中的排名（已批阅成绩中比 ta 高的人数 + 1，同分并列） */
    @Select("SELECT COUNT(*) + 1 FROM grade WHERE task_id = #{taskId} AND status = 'GRADED' " +
            "AND total_score > #{totalScore}")
    int countRank(Long taskId, Integer totalScore);

    /** 某任务已提交学生总数 */
    @Select("SELECT COUNT(*) FROM grade WHERE task_id = #{taskId}")
    int countSubmitted(Long taskId);

    /** 某任务已完成批阅的学生数（排名分母） */
    @Select("SELECT COUNT(*) FROM grade WHERE task_id = #{taskId} AND status = 'GRADED'")
    int countGraded(Long taskId);

    /** 某任务的所有成绩记录 */
    @Select("SELECT * FROM grade WHERE task_id = #{taskId}")
    List<com.example.onlinecourseai.entity.Grade> findByTaskId(Long taskId);

    /** 某任务班级总人数 */
    @Select("SELECT COUNT(*) FROM enrollment WHERE class_id = (SELECT class_id FROM task WHERE id = #{taskId})")
    int classStudentCount(Long taskId);
}
