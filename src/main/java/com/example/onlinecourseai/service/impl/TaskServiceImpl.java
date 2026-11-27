package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.QuestionVO;
import com.example.onlinecourseai.dto.StudyStatusVO;
import com.example.onlinecourseai.dto.TaskDetailVO;
import com.example.onlinecourseai.dto.TaskVO;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.ClassInfo;
import com.example.onlinecourseai.entity.Enrollment;
import com.example.onlinecourseai.entity.Question;
import com.example.onlinecourseai.entity.Task;
import com.example.onlinecourseai.entity.TaskStudyRecord;
import com.example.onlinecourseai.entity.TaskSubmitExtension;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.mapper.AnswerRecordMapper;
import com.example.onlinecourseai.mapper.ClassInfoMapper;
import com.example.onlinecourseai.mapper.EnrollmentMapper;
import com.example.onlinecourseai.mapper.QuestionMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.mapper.TaskStudyMapper;
import com.example.onlinecourseai.mapper.TaskSubmitExtensionMapper;
import com.example.onlinecourseai.service.TaskService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 教学任务业务：教师发布/编辑/删除/搜索任务，学生查收任务，查看详情
 */
@Service
public class TaskServiceImpl implements TaskService {

    private final TaskMapper taskMapper;
    private final ClassInfoMapper classInfoMapper;
    private final AnimationMapper animationMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerMapper;
    private final TaskStudyMapper taskStudyMapper;
    private final TaskSubmitExtensionMapper extensionMapper;

    public TaskServiceImpl(TaskMapper taskMapper,
                           ClassInfoMapper classInfoMapper,
                           AnimationMapper animationMapper,
                           EnrollmentMapper enrollmentMapper,
                           QuestionMapper questionMapper,
                           AnswerRecordMapper answerMapper,
                           TaskStudyMapper taskStudyMapper,
                           TaskSubmitExtensionMapper extensionMapper) {
        this.taskMapper = taskMapper;
        this.classInfoMapper = classInfoMapper;
        this.animationMapper = animationMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.taskStudyMapper = taskStudyMapper;
        this.extensionMapper = extensionMapper;
    }

    @Override
    public TaskVO publishTask(Long teacherId, String title, Long classId, Long animationId, LocalDateTime deadline) {
        if (title == null || title.isBlank()) {
            throw new BizException("任务标题不能为空");
        }
        if (classId == null || animationId == null) {
            throw new BizException("班级和课件不能为空");
        }

        ClassInfo clazz = classInfoMapper.findById(classId);
        if (clazz == null) {
            throw new BizException("班级不存在");
        }
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BizException("只能往自己创建的班级发布任务");
        }

        Animation animation = animationMapper.findById(animationId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if (!animation.getTeacherId().equals(teacherId)) {
            throw new BizException("只能发布自己生成的课件");
        }
        // 同一班级同一课件只保留一条任务，避免“一条交完、另一条永远未完成”
        if (taskMapper.countByClassAndAnimation(classId, animationId) > 0) {
            throw new BizException("该班级已发布过这份课件，请勿重复发布");
        }

        Task task = new Task();
        task.setTitle(title.trim());
        task.setClassId(classId);
        task.setAnimationId(animationId);
        task.setTeacherId(teacherId);
        task.setDeadline(deadline);
        task.setStatus("ACTIVE");
        task.setCreateTime(LocalDateTime.now());
        try {
            taskMapper.insert(task);
        } catch (DuplicateKeyException e) {
            // 并发重复提交时由数据库唯一索引兜底
            throw new BizException("该班级已发布过这份课件，请勿重复发布");
        }

        return buildTaskVO(task, clazz.getClassName(), animation.getTitle());
    }

    @Override
    public List<TaskVO> getMyTasks(Long userId, String role) {
        if ("teacher".equals(role)) {
            return taskMapper.findByTeacherId(userId);
        }
        return taskMapper.findByStudentClasses(userId);
    }

    @Override
    public TaskDetailVO getTaskDetail(Long userId, String role, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }

        ClassInfo clazz = classInfoMapper.findById(task.getClassId());
        if ("teacher".equals(role)) {
            if (!clazz.getTeacherId().equals(userId)) {
                throw new BizException("只能查看自己发布的任务");
            }
        } else {
            Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), userId);
            if (enrollment == null) {
                throw new BizException("你不在该任务的班级中，无法查看");
            }
        }

        Animation animation = animationMapper.findById(task.getAnimationId());
        boolean teacherView = "teacher".equals(role);

        // 不再强制先学后练：学生进入任务即可作答，题目直接下发；
        // 参考答案仍仅教师可见，学生提交作答后经 /my-result 展示。
        List<Question> questions = questionMapper.findByAnimationId(task.getAnimationId());

        TaskDetailVO detail = new TaskDetailVO();
        detail.setTask(buildTaskVO(task, clazz.getClassName(), animation.getTitle()));
        detail.setContentHtml(animation.getContentHtml());
        detail.setStudied(true);
        detail.setMinStudySeconds(0);
        detail.setStudySeconds(0);

        List<QuestionVO> questionVOs = new ArrayList<>();
        for (Question q : questions) {
            QuestionVO qv = new QuestionVO();
            qv.setId(q.getId());
            qv.setQuestionText(q.getQuestionText());
            qv.setOptions(q.getOptions());
            // 学生端只在提交后通过 /my-result 获取参考答案，任务详情不提前下发
            qv.setAnswer(teacherView ? q.getAnswer() : null);
            qv.setQuestionType(q.getQuestionType());
            qv.setScore(q.getScore());
            questionVOs.add(qv);
        }
        detail.setQuestions(questionVOs);
        if (!teacherView) {
            TaskSubmitExtension ext = extensionMapper.findByTaskAndStudent(taskId, userId);
            if (ext != null) {
                detail.setExtendDeadline(ext.getExtendDeadline());
            }
        }
        return detail;
    }

    @Override
    public StudyStatusVO startStudy(Long studentId, Long taskId) {
        checkStudentTaskAccess(studentId, taskId);
        taskStudyMapper.start(taskId, studentId);
        return buildStudyStatus(taskId, studentId);
    }

    @Override
    public StudyStatusVO heartbeatStudy(Long studentId, Long taskId) {
        checkStudentTaskAccess(studentId, taskId);
        TaskStudyRecord record = taskStudyMapper.find(taskId, studentId);
        if (record == null || record.getFirstViewAt() == null) {
            taskStudyMapper.start(taskId, studentId);
        }
        // 前端每 15 秒发一次心跳，只累加当前心跳代表的 15 秒；封顶为首次进入至今的自然时长
        taskStudyMapper.addSeconds(taskId, studentId, 15);
        return buildStudyStatus(taskId, studentId);
    }

    @Override
    public StudyStatusVO confirmStudy(Long studentId, Long taskId) {
        checkStudentTaskAccess(studentId, taskId);
        TaskStudyRecord record = taskStudyMapper.find(taskId, studentId);
        int seconds = record == null || record.getStudySeconds() == null
                ? 0 : Math.max(0, record.getStudySeconds());
        if (record == null || record.getFirstViewAt() == null) {
            taskStudyMapper.start(taskId, studentId);
        }
        // 已放开时长门禁：不再校验最少停留秒数，直接标记确认
        taskStudyMapper.confirm(taskId, studentId, seconds);
        return buildStudyStatus(taskId, studentId);
    }

    private StudyStatusVO buildStudyStatus(Long taskId, Long studentId) {
        TaskStudyRecord record = taskStudyMapper.find(taskId, studentId);
        StudyStatusVO vo = new StudyStatusVO();
        vo.setStudied(record != null && record.getConfirmed() != null && record.getConfirmed() == 1);
        vo.setStudySeconds(record == null || record.getStudySeconds() == null ? 0
                : Math.max(0, record.getStudySeconds()));
        vo.setMinStudySeconds(0);
        return vo;
    }

    private void checkStudentTaskAccess(Long studentId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        if ("CLOSED".equals(task.getStatus())) {
            throw new BizException("任务已关闭，无法继续学习");
        }
        Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), studentId);
        if (enrollment == null) {
            throw new BizException("你不在该任务的班级中，无法学习");
        }
    }

    @Override
    public TaskVO updateTask(Long teacherId, Long taskId, String title, Long classId, LocalDateTime deadline) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        ClassInfo clazz = classInfoMapper.findById(task.getClassId());
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BizException("只能修改自己发布的任务");
        }
        if (title == null || title.isBlank()) {
            throw new BizException("任务标题不能为空");
        }
        // 已发布任务不允许更换班级：旧班学生的成绩会挂在原任务上，新班学生又没有提交记录
        if (classId != null && !classId.equals(task.getClassId())) {
            throw new BizException("任务发布后不能更换班级，请关闭后重新发布");
        }
        taskMapper.updateTask(taskId, title.trim(), deadline, task.getClassId());
        Task updated = taskMapper.findById(taskId);
        ClassInfo updatedClazz = classInfoMapper.findById(updated.getClassId());
        Animation animation = animationMapper.findById(updated.getAnimationId());
        return buildTaskVO(updated, updatedClazz.getClassName(), animation.getTitle());
    }

    @Override
    public void deleteTask(Long teacherId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        ClassInfo clazz = classInfoMapper.findById(task.getClassId());
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BizException("只能删除自己发布的任务");
        }
        // 有学生提交作答的任务不能直接删，先关闭
        int submitted = answerMapper.countSubmissionByTaskId(taskId);
        if (submitted > 0) {
            throw new BizException("该任务已有 " + submitted + " 人提交作业，不能删除，可先关闭任务");
        }
        taskMapper.deleteById(taskId);
        taskStudyMapper.deleteByTaskId(taskId);
    }

    @Override
    public TaskVO toggleTaskStatus(Long teacherId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        ClassInfo clazz = classInfoMapper.findById(task.getClassId());
        if (!clazz.getTeacherId().equals(teacherId)) {
            throw new BizException("只能操作自己发布的任务");
        }
        String newStatus = "ACTIVE".equals(task.getStatus()) ? "CLOSED" : "ACTIVE";
        taskMapper.updateStatus(taskId, newStatus);
        Task updated = taskMapper.findById(taskId);
        Animation animation = animationMapper.findById(updated.getAnimationId());
        return buildTaskVO(updated, clazz.getClassName(), animation.getTitle());
    }

    @Override
    public List<TaskVO> searchTasks(Long teacherId, String keyword, Long classId) {
        return taskMapper.searchTasks(teacherId, keyword, classId);
    }

    private TaskVO buildTaskVO(Task task, String className, String animationTitle) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setClassId(task.getClassId());
        vo.setClassName(className);
        vo.setAnimationId(task.getAnimationId());
        vo.setAnimationTitle(animationTitle);
        vo.setDeadline(task.getDeadline());
        vo.setStatus(task.getStatus());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }
}
