package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.Animation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课件表 Mapper
 */
@Mapper
public interface AnimationMapper {

    @Insert("INSERT INTO animation(title, knowledge_point, summary, content_html, status, teacher_id, created_at) " +
            "VALUES(#{title}, #{knowledgePoint}, #{summary}, #{contentHtml}, #{status}, #{teacherId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Animation animation);

    @Select("SELECT * FROM animation WHERE id = #{id}")
    Animation findById(Long id);

    @Select("SELECT * FROM animation WHERE teacher_id = #{teacherId} ORDER BY created_at DESC")
    List<Animation> findByTeacherId(Long teacherId);

    @Update("UPDATE animation SET summary = #{summary} WHERE id = #{id}")
    int updateSummary(Long id, String summary);

    /** 更新课件标题与摘要 */
    @Update("UPDATE animation SET title = #{title}, summary = #{summary} WHERE id = #{id}")
    int updateFields(Long id, String title, String summary);

    /** 更新课件完整 HTML（教师修改题目答案后同步判分脚本） */
    @Update("UPDATE animation SET content_html = #{contentHtml} WHERE id = #{id}")
    int updateContentHtml(Long id, String contentHtml);

    @Delete("DELETE FROM animation WHERE id = #{id}")
    int deleteById(Long id);

    /** 课件归属到某门课程（智能归科时回写） */
    @Update("UPDATE animation SET course_id = #{courseId} WHERE id = #{id}")
    int updateCourseId(@Param("id") Long id, @Param("courseId") Long courseId);

    /** 某门课程下的课件数量 */
    @Select("SELECT COUNT(*) FROM animation WHERE course_id = #{courseId}")
    int countByCourseId(Long courseId);

    /** 某门课程下的课件（不返回 content_html，避免大字段传输） */
    @Select("SELECT id, title, knowledge_point, summary, status, teacher_id, course_id, created_at " +
            "FROM animation WHERE course_id = #{courseId} ORDER BY created_at DESC")
    List<Animation> findByCourseId(Long courseId);

    /** 教师名下未归科的课件数量 */
    @Select("SELECT COUNT(*) FROM animation WHERE teacher_id = #{teacherId} AND (course_id IS NULL OR course_id = 0)")
    int countUnassigned(Long teacherId);

    /** 解除某门课程下所有课件的归属（删除课程前调用，不删除课件本身） */
    @Update("UPDATE animation SET course_id = NULL WHERE course_id = #{courseId}")
    int clearCourseId(Long courseId);
}
