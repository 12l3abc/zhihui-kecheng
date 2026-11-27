package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.AnswerItemDTO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.dto.StudentAnswerVO;
import com.example.onlinecourseai.dto.StudentTaskResultVO;
import com.example.onlinecourseai.dto.SubmitResultVO;
import com.example.onlinecourseai.entity.Grade;

import java.util.List;
import java.util.Map;

public interface AnswerService {

    /** 学生提交任务作答（客观题自动判分，主观题待教师批阅） */
    SubmitResultVO submitAnswers(Long studentId, Long taskId, List<AnswerItemDTO> answers);

    /** 教师查看任务批阅总览（每个学生的提交与得分） */
    List<ReviewStudentVO> getReviewOverview(Long teacherId, Long taskId);

    /** 教师查看某学生的逐题作答详情 */
    List<StudentAnswerVO> getStudentAnswers(Long teacherId, Long taskId, Long studentId);

    /** 教师批阅某学生：填写主观题得分与评语；answers 为教师修改后的逐题答案（客观重判、主观回写文本） */
    Grade gradeStudent(Long teacherId, Long taskId, Long studentId, Integer teacherScore, String comment, List<AnswerItemDTO> answers);

    /** 教师打回重做：清空该生本次作答与成绩；extendDeadline 非空时为其单独顺延补交截止 */
    void rejectStudent(Long teacherId, Long taskId, Long studentId, java.time.LocalDateTime extendDeadline);

    /** 教师为未提交学生单独顺延补交截止（不清空作答，学生在新窗口内解锁提交） */
    void extendDeadline(Long teacherId, Long taskId, Long studentId, java.time.LocalDateTime extendDeadline);

    /** 教师催交：给未提交学生写一条催交提醒 */
    void remindStudent(Long teacherId, Long taskId, Long studentId);

    /** 学生在某任务上是否有未读催交提醒 */
    boolean hasUnreadRemind(Long studentId, Long taskId);

    /** 学生所有未读催交涉及的任务 id（不标已读，供任务列表红点） */
    List<Long> listUnreadRemindTaskIds(Long studentId);

    /** 学生收到的催交通知明细列表（含已读，供通知栏目） */
    List<Map<String, Object>> listMyNotices(Long studentId);

    /** 学生将某任务的催交提醒标记已读 */
    void readReminds(Long studentId, Long taskId);

    /** 学生查看自己的提交结果；未提交时不返回参考答案 */
    StudentTaskResultVO getMyResult(Long studentId, Long taskId);
}
