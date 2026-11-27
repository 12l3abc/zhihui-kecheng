package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.MyGradeVO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.entity.ClassInfo;
import com.example.onlinecourseai.entity.Task;
import com.example.onlinecourseai.mapper.ClassInfoMapper;
import com.example.onlinecourseai.mapper.GradeMapper;
import com.example.onlinecourseai.mapper.QuestionMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.service.GradeService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成绩模块业务：学生查成绩+班级排名；教师查任务成绩单
 */
@Service
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;
    private final TaskMapper taskMapper;
    private final ClassInfoMapper classInfoMapper;
    private final QuestionMapper questionMapper;

    public GradeServiceImpl(GradeMapper gradeMapper, TaskMapper taskMapper,
                             ClassInfoMapper classInfoMapper, QuestionMapper questionMapper) {
        this.gradeMapper = gradeMapper;
        this.taskMapper = taskMapper;
        this.classInfoMapper = classInfoMapper;
        this.questionMapper = questionMapper;
    }

    @Override
    public List<MyGradeVO> getMyGrades(Long studentId) {
        List<MyGradeVO> list = gradeMapper.findMyGrades(studentId);
        for (MyGradeVO vo : list) {
            // 只有完成批阅的成绩参与排名；REVIEWING 的部分分不参与，避免排名随后续批阅跳动
            if ("GRADED".equals(vo.getStatus())) {
                vo.setRank(gradeMapper.countRank(vo.getTaskId(), vo.getTotalScore()));
                vo.setTotalStudents(gradeMapper.countGraded(vo.getTaskId()));
            } else {
                vo.setRank(null);
                vo.setTotalStudents(gradeMapper.countGraded(vo.getTaskId()));
            }
        }
        return list;
    }

    @Override
    public List<ReviewStudentVO> getTaskRanking(Long teacherId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        Long ownerId = classInfoMapper.findById(task.getClassId()).getTeacherId();
        if (!ownerId.equals(teacherId)) {
            throw new BizException("只能查看自己发布的任务成绩");
        }

        // 与 countRank 一致：同分并列，后续名次按人数顺延；仅 GRADED 参与
        List<ReviewStudentVO> list = gradeMapper.findReviewOverview(taskId);
        int position = 0;
        int lastRank = 0;
        Integer lastScore = null;
        for (ReviewStudentVO vo : list) {
            if (vo.getSubmitted() == null || vo.getSubmitted() != 1
                    || !"GRADED".equals(vo.getStatus()) || vo.getTotalScore() == null) {
                vo.setRank(null);
                continue;
            }
            position++;
            if (lastScore == null || !lastScore.equals(vo.getTotalScore())) {
                lastRank = position;
                lastScore = vo.getTotalScore();
            }
            vo.setRank(lastRank);
        }
        return list;
    }

    @Override
    public Task getTaskEntity(Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        return task;
    }

    @Override
    public ClassInfo getClassInfo(Long classId) {
        ClassInfo clazz = classInfoMapper.findById(classId);
        if (clazz == null) {
            throw new BizException("班级不存在");
        }
        return clazz;
    }

    @Override
    public Map<String, Object> getScoreDistribution(Long taskId) {
        Map<String, Object> result = new HashMap<>();
        // 查任务满分（题目总分）
        Task task = taskMapper.findById(taskId);
        int fullScore = questionMapper.sumScoreByAnimationId(task.getAnimationId());
        result.put("fullScore", fullScore);
        // 只统计已完成批阅（GRADED）的成绩，和首页看板 avgGradedScore 口径对齐
        List<com.example.onlinecourseai.entity.Grade> grades = gradeMapper.findByTaskId(taskId);
        int gradedCount = 0;
        int excellent = 0, good = 0, average = 0, pass = 0, fail = 0;
        double sum = 0;
        int max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;
        for (com.example.onlinecourseai.entity.Grade g : grades) {
            if (!"GRADED".equals(g.getStatus())) {
                continue;
            }
            gradedCount++;
            int score = g.getTotalScore() != null ? g.getTotalScore() : 0;
            sum += score;
            if (score > max) max = score;
            if (score < min) min = score;
            if (fullScore > 0) {
                double ratio = (double) score / fullScore;
                if (ratio >= 0.9) excellent++;
                else if (ratio >= 0.8) good++;
                else if (ratio >= 0.7) average++;
                else if (ratio >= 0.6) pass++;
                else fail++;
            } else {
                // 没题目时按0分算不及格
                fail++;
            }
        }
        result.put("excellent", excellent);
        result.put("good", good);
        result.put("average", average);
        result.put("pass", pass);
        result.put("fail", fail);
        if (gradedCount > 0) {
            result.put("avgScore", Math.round(sum / gradedCount * 10) / 10.0);
            result.put("maxScore", max);
            result.put("minScore", min);
        } else {
            result.put("avgScore", null);
            result.put("maxScore", null);
            result.put("minScore", null);
        }
        return result;
    }

    @Override
    public int getClassStudentCount(Long taskId) {
        return gradeMapper.classStudentCount(taskId);
    }

    @Override
    public int getSubmittedCount(Long taskId) {
        return gradeMapper.countSubmitted(taskId);
    }
}
