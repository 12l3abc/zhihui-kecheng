package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.CoursewareFeedback;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课件评价与疑问 Mapper
 */
@Mapper
public interface CoursewareFeedbackMapper {

    @Insert("INSERT INTO courseware_feedback(animation_id, student_id, student_name, type, content, created_at) " +
            "VALUES(#{animationId}, #{studentId}, #{studentName}, #{type}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CoursewareFeedback feedback);

    /** 按课件ID查询所有评价/疑问，按时间倒序 */
    @Select("SELECT * FROM courseware_feedback WHERE animation_id = #{animationId} ORDER BY created_at DESC")
    List<CoursewareFeedback> findByAnimationId(Long animationId);

    /** 按学生ID查询自己提交的所有评价/疑问 */
    @Select("SELECT * FROM courseware_feedback WHERE student_id = #{studentId} ORDER BY created_at DESC")
    List<CoursewareFeedback> findByStudentId(Long studentId);

    /** 教师查询自己所有课件的评价/疑问（通过 animation.teacher_id 关联），附带学生所在班级 */
    @Select("SELECT f.*, " +
            "  (SELECT c.class_name FROM enrollment e " +
            "   JOIN class_info c ON e.class_id = c.id " +
            "   WHERE e.student_id = f.student_id LIMIT 1) AS class_name " +
            "FROM courseware_feedback f " +
            "INNER JOIN animation a ON f.animation_id = a.id " +
            "WHERE a.teacher_id = #{teacherId} " +
            "ORDER BY f.created_at DESC")
    List<CoursewareFeedback> findByTeacherId(Long teacherId);

    /** 统计某个课件的评价数量 */
    @Select("SELECT COUNT(*) FROM courseware_feedback WHERE animation_id = #{animationId} AND type = 'evaluation'")
    int countEvaluationByAnimationId(Long animationId);

    /** 统计某个课件的疑问数量 */
    @Select("SELECT COUNT(*) FROM courseware_feedback WHERE animation_id = #{animationId} AND type = 'question'")
    int countQuestionByAnimationId(Long animationId);

    /** 按主键查询单条反馈（用于归属校验） */
    @Select("SELECT * FROM courseware_feedback WHERE id = #{id}")
    CoursewareFeedback findById(Long id);

    /** 更新反馈内容（仅本人，由 Service 层校验归属） */
    @Update("UPDATE courseware_feedback SET content = #{content} WHERE id = #{id}")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    /** 按主键删除反馈（仅本人，由 Service 层校验归属） */
    @Delete("DELETE FROM courseware_feedback WHERE id = #{id}")
    int deleteById(Long id);
}
