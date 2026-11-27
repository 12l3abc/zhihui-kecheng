package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.TaskSubmitExtension;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 学生单独补交延期 Mapper
 */
@Mapper
public interface TaskSubmitExtensionMapper {

    /** 查某学生在某任务上的单独延期记录 */
    @Select("SELECT * FROM task_submit_extension WHERE task_id = #{taskId} AND student_id = #{studentId}")
    TaskSubmitExtension findByTaskAndStudent(Long taskId, Long studentId);

    @Insert("INSERT INTO task_submit_extension(task_id, student_id, extend_deadline) " +
            "VALUES(#{taskId}, #{studentId}, #{extendDeadline})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskSubmitExtension extension);

    @Update("UPDATE task_submit_extension SET extend_deadline = #{extendDeadline} " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId}")
    int updateDeadline(TaskSubmitExtension extension);

    /** 清除某学生在某任务上的单独延期 */
    @Delete("DELETE FROM task_submit_extension WHERE task_id = #{taskId} AND student_id = #{studentId}")
    int deleteByTaskAndStudent(Long taskId, Long studentId);
}
