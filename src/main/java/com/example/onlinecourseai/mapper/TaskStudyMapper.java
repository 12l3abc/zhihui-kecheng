package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.TaskStudyRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskStudyMapper {

    @Select("SELECT * FROM task_study_record WHERE task_id = #{taskId} AND student_id = #{studentId}")
    TaskStudyRecord find(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    /** 首次打开课件时写入；已存在则只补齐首次进入时间，不重置学习进度 */
    @Insert("INSERT INTO task_study_record(task_id, student_id, study_seconds, confirmed, first_view_at) " +
            "VALUES(#{taskId}, #{studentId}, 0, 0, NOW()) " +
            "ON DUPLICATE KEY UPDATE first_view_at = COALESCE(first_view_at, NOW())")
    int start(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    /** 学生确认学完；服务端在调用前校验停留时长 */
    @Update("UPDATE task_study_record SET confirmed = 1, " +
            "study_seconds = GREATEST(study_seconds, #{seconds}), confirmed_at = NOW() " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId}")
    int confirm(@Param("taskId") Long taskId, @Param("studentId") Long studentId, @Param("seconds") int seconds);

    @Select("SELECT COUNT(*) FROM task_study_record WHERE task_id = #{taskId} " +
            "AND student_id = #{studentId} AND confirmed = 1")
    int isConfirmed(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    /** 从首次打开课件到现在已经停留的秒数 */
    @Select("SELECT TIMESTAMPDIFF(SECOND, first_view_at, NOW()) FROM task_study_record " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId}")
    Integer elapsedSeconds(@Param("taskId") Long taskId, @Param("studentId") Long studentId);

    /** 有效学习心跳：只累加本次心跳代表的时长，且不超过首次进入至今的自然时间 */
    @Update("UPDATE task_study_record SET study_seconds = " +
            "LEAST(TIMESTAMPDIFF(SECOND, first_view_at, NOW()), study_seconds + #{seconds}) " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId} AND confirmed = 0")
    int addSeconds(@Param("taskId") Long taskId, @Param("studentId") Long studentId,
                   @Param("seconds") int seconds);

    @Delete("DELETE FROM task_study_record WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") Long taskId);
}
