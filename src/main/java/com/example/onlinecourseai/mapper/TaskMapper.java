package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.dto.TaskVO;
import com.example.onlinecourseai.entity.Task;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 教学任务表 Mapper
 */
@Mapper
public interface TaskMapper {

    @Insert("INSERT INTO task(title, class_id, animation_id, teacher_id, deadline, status, create_time) " +
            "VALUES(#{title}, #{classId}, #{animationId}, #{teacherId}, #{deadline}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Task task);

    @Select("SELECT * FROM task WHERE id = #{id}")
    Task findById(Long id);

    /** 教师发布的全部任务（按任务真实发布者过滤，关联班级名与课件标题） */
    @Select("SELECT t.id, t.title, t.class_id, c.class_name, t.animation_id, a.title AS animation_title, " +
            "t.deadline, t.status, t.create_time " +
            "FROM task t " +
            "JOIN class_info c ON t.class_id = c.id " +
            "JOIN animation a ON t.animation_id = a.id " +
            "WHERE t.teacher_id = #{teacherId} " +
            "ORDER BY t.create_time DESC")
    List<TaskVO> findByTeacherId(Long teacherId);

    /** 学生所在班级收到的任务（通过选课表查班级；带出该生单独顺延截止） */
    @Select("SELECT t.id, t.title, t.class_id, c.class_name, t.animation_id, a.title AS animation_title, " +
            "t.deadline, t.status, t.create_time, e.extend_deadline " +
            "FROM task t " +
            "JOIN class_info c ON t.class_id = c.id " +
            "JOIN animation a ON t.animation_id = a.id " +
            "LEFT JOIN task_submit_extension e ON e.task_id = t.id AND e.student_id = #{studentId} " +
            "WHERE t.class_id IN (SELECT class_id FROM enrollment WHERE student_id = #{studentId}) " +
            "ORDER BY t.create_time DESC")
    List<TaskVO> findByStudentClasses(Long studentId);

    /** 某课件被任务引用的数量（删除课件前的安全校验） */
    @Select("SELECT COUNT(*) FROM task WHERE animation_id = #{animationId}")
    int countByAnimationId(Long animationId);

    /** 同班同课件已发布的任务数（防重复发布） */
    @Select("SELECT COUNT(*) FROM task WHERE class_id = #{classId} AND animation_id = #{animationId}")
    int countByClassAndAnimation(@Param("classId") Long classId, @Param("animationId") Long animationId);

    /** 某课件关联的全部任务（改客观题答案后重算历史判分用） */
    @Select("SELECT * FROM task WHERE animation_id = #{animationId}")
    List<Task> findByAnimationId(Long animationId);

    /** 学生是否可通过本人所在班级的任务访问该课件（课件详情越权防护用） */
    @Select("SELECT COUNT(*) FROM task WHERE animation_id = #{animationId} " +
            "AND class_id IN (SELECT class_id FROM enrollment WHERE student_id = #{studentId})")
    int countAccessibleByStudent(@Param("animationId") Long animationId, @Param("studentId") Long studentId);

    /** 某班级的任务数量（删除班级前的安全校验） */
    @Select("SELECT COUNT(*) FROM task WHERE class_id = #{classId}")
    int countByClassId(Long classId);

    /** 班级合并：把源班级的任务迁移到目标班级 */
    @Update("UPDATE task SET class_id = #{newClassId} WHERE class_id = #{oldClassId}")
    int moveTasks(Long oldClassId, Long newClassId);

    /** 修改任务标题、截止时间、班级 */
    @Update("UPDATE task SET title = #{title}, deadline = #{deadline}, class_id = #{classId} WHERE id = #{id}")
    int updateTask(@Param("id") Long id, @Param("title") String title,
                   @Param("deadline") java.time.LocalDateTime deadline,
                   @Param("classId") Long classId);

    /** 修改任务状态 */
    @Update("UPDATE task SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /** 删除任务 */
    @Delete("DELETE FROM task WHERE id = #{id}")
    int deleteById(Long id);

    /** 教师搜索任务（标题模糊 + 班级筛选） */
    @Select("<script>" +
            "SELECT t.id, t.title, t.class_id, c.class_name, t.animation_id, a.title AS animation_title, " +
            "t.deadline, t.status, t.create_time " +
            "FROM task t " +
            "JOIN class_info c ON t.class_id = c.id " +
            "JOIN animation a ON t.animation_id = a.id " +
            "WHERE t.teacher_id = #{teacherId} " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND t.title LIKE CONCAT('%', #{keyword}, '%') " +
            "</if>" +
            "<if test='classId != null'>" +
            "  AND t.class_id = #{classId} " +
            "</if>" +
            "ORDER BY t.create_time DESC" +
            "</script>")
    List<TaskVO> searchTasks(@Param("teacherId") Long teacherId,
                             @Param("keyword") String keyword,
                             @Param("classId") Long classId);
}
