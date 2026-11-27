package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.TaskRemind;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 教师催交提醒 Mapper
 */
@Mapper
public interface TaskRemindMapper {

    @Insert("INSERT INTO task_remind(task_id, student_id, teacher_id, message, read_flag, created_at) " +
            "VALUES(#{taskId}, #{studentId}, #{teacherId}, #{message}, 0, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TaskRemind remind);

    /** 某学生在某任务上的最新一条催交记录（用于去重更新，不重复堆记录） */
    @Select("SELECT * FROM task_remind WHERE task_id = #{taskId} AND student_id = #{studentId} " +
            "ORDER BY id DESC LIMIT 1")
    TaskRemind findLatestByTaskAndStudent(Long taskId, Long studentId);

    /** 复用已有催交记录：刷新时间并重新置为未读 */
    @Update("UPDATE task_remind SET teacher_id = #{teacherId}, message = #{message}, " +
            "read_flag = 0, created_at = #{createdAt} WHERE id = #{id}")
    int refresh(TaskRemind remind);

    /** 某学生在某任务上的未读催交条数 */
    @Select("SELECT COUNT(*) FROM task_remind " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId} AND read_flag = 0")
    int countUnreadByTaskAndStudent(Long taskId, Long studentId);

    /** 将某学生在某任务上的未读催交标记已读 */
    @Update("UPDATE task_remind SET read_flag = 1 " +
            "WHERE task_id = #{taskId} AND student_id = #{studentId} AND read_flag = 0")
    int markReadByTaskAndStudent(Long taskId, Long studentId);

    /** 某学生所有未读催交涉及的任务 id（不去标已读，供列表红点） */
    @Select("SELECT DISTINCT task_id FROM task_remind " +
            "WHERE student_id = #{studentId} AND read_flag = 0")
    List<Long> selectUnreadTaskIds(Long studentId);

    /**
     * 某学生收到的催交记录（供通知栏目）。
     * 历史数据存在同一任务同一学生的重复行，这里只取每个任务最新一条，避免通知列表重复刷屏。
     */
    @Select("SELECT r.* FROM task_remind r " +
            "WHERE r.student_id = #{studentId} " +
            "AND r.id = (SELECT MAX(r2.id) FROM task_remind r2 " +
            " WHERE r2.task_id = r.task_id AND r2.student_id = r.student_id) " +
            "ORDER BY r.created_at DESC")
    List<TaskRemind> selectAllByStudent(Long studentId);
}
