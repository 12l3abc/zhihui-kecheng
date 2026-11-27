package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.TeacherNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 教师通知表 Mapper
 */
@Mapper
public interface TeacherNotificationMapper {

    @Insert("INSERT INTO teacher_notification(teacher_id, type, title, content, ref_task_id, " +
            "ref_student_id, ref_animation_id, read_flag, created_at) " +
            "VALUES(#{teacherId}, #{type}, #{title}, #{content}, #{refTaskId}, " +
            "#{refStudentId}, #{refAnimationId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeacherNotification notification);

    /** 教师通知列表：未读在前、时间倒序，JOIN 学生姓名 */
    @Select("SELECT n.*, IFNULL(u.real_name, u.username) AS student_name " +
            "FROM teacher_notification n " +
            "LEFT JOIN sys_user u ON u.id = n.ref_student_id " +
            "WHERE n.teacher_id = #{teacherId} " +
            "ORDER BY n.read_flag ASC, n.created_at DESC")
    List<TeacherNotification> findByTeacherId(Long teacherId);

    /** 未读通知条数 */
    @Select("SELECT COUNT(*) FROM teacher_notification WHERE teacher_id = #{teacherId} AND read_flag = 0")
    int countUnread(Long teacherId);

    /** 标记单条已读（限定本人） */
    @Update("UPDATE teacher_notification SET read_flag = 1 WHERE id = #{id} AND teacher_id = #{teacherId}")
    int markRead(@Param("id") Long id, @Param("teacherId") Long teacherId);

    /** 全部标记已读 */
    @Update("UPDATE teacher_notification SET read_flag = 1 WHERE teacher_id = #{teacherId} AND read_flag = 0")
    int markAllRead(Long teacherId);
}
