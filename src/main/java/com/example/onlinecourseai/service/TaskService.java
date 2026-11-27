package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.StudyStatusVO;
import com.example.onlinecourseai.dto.TaskDetailVO;
import com.example.onlinecourseai.dto.TaskVO;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskService {

    /** 教师把课件发布为任务到指定班级 */
    TaskVO publishTask(Long teacherId, String title, Long classId, Long animationId, LocalDateTime deadline);

    /** 我的任务（教师：我发布的；学生：我所在班级收到的） */
    List<TaskVO> getMyTasks(Long userId, String role);

    /** 任务详情（含课件动画与题目，校验访问权限） */
    TaskDetailVO getTaskDetail(Long userId, String role, Long taskId);

    /** 学生首次打开任务课件，开始累计学习时长 */
    StudyStatusVO startStudy(Long studentId, Long taskId);

    /** 课件页学习心跳：累加有效停留秒数，关闭页面后不再增长 */
    StudyStatusVO heartbeatStudy(Long studentId, Long taskId);

    /** 学生确认学完课件；有效停留时长不足会被拒绝 */
    StudyStatusVO confirmStudy(Long studentId, Long taskId);

    /** 教师修改任务（标题/截止时间/班级） */
    TaskVO updateTask(Long teacherId, Long taskId, String title, Long classId, LocalDateTime deadline);

    /** 教师删除任务 */
    void deleteTask(Long teacherId, Long taskId);

    /** 教师切换任务状态（开启/关闭） */
    TaskVO toggleTaskStatus(Long teacherId, Long taskId);

    /** 教师搜索自己的任务（按标题关键词、按班级筛选） */
    List<TaskVO> searchTasks(Long teacherId, String keyword, Long classId);
}
