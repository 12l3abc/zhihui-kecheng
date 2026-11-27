package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.MyGradeVO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.entity.ClassInfo;
import com.example.onlinecourseai.entity.Task;

import java.util.List;
import java.util.Map;

public interface GradeService {

    /** 学生查看自己的成绩列表（含班级排名） */
    List<MyGradeVO> getMyGrades(Long studentId);

    /** 教师查看某任务的班级成绩单（按总分排名） */
    List<ReviewStudentVO> getTaskRanking(Long teacherId, Long taskId);

    /** 获取任务实体 */
    Task getTaskEntity(Long taskId);

    /** 获取班级实体 */
    ClassInfo getClassInfo(Long classId);

    /** 某任务成绩分数段分布 */
    Map<String, Object> getScoreDistribution(Long taskId);

    /** 某任务班级总人数 */
    int getClassStudentCount(Long taskId);

    /** 某任务已提交人数 */
    int getSubmittedCount(Long taskId);
}
