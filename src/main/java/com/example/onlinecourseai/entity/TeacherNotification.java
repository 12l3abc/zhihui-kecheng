package com.example.onlinecourseai.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师通知表 teacher_notification
 * 学生提交作业、学生课件反馈等事件推送给发布教师
 */
@Data
public class TeacherNotification {

    private Long id;

    /** 接收通知的教师（任务发布者 / 课件作者） */
    private Long teacherId;

    /** 类型：SUBMIT 作业提交 / FEEDBACK 课件反馈 */
    private String type;

    private String title;

    private String content;

    /** 关联任务ID（SUBMIT） */
    private Long refTaskId;

    /** 关联学生ID */
    private Long refStudentId;

    /** 关联课件ID（FEEDBACK） */
    private Long refAnimationId;

    /** 是否已读：0未读 / 1已读 */
    private Integer readFlag;

    private LocalDateTime createdAt;

    /** 非数据库字段：学生姓名（JOIN 返回） */
    private String studentName;
}
