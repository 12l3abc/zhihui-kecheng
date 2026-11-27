package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.AnswerItemDTO;
import com.example.onlinecourseai.dto.ReviewStudentVO;
import com.example.onlinecourseai.dto.StudentAnswerVO;
import com.example.onlinecourseai.dto.StudentTaskResultVO;
import com.example.onlinecourseai.dto.SubmitResultVO;
import com.example.onlinecourseai.entity.AnswerRecord;
import com.example.onlinecourseai.entity.Enrollment;
import com.example.onlinecourseai.entity.Grade;
import com.example.onlinecourseai.entity.Question;
import com.example.onlinecourseai.entity.Task;
import com.example.onlinecourseai.entity.TaskStudyRecord;
import com.example.onlinecourseai.entity.TaskRemind;
import com.example.onlinecourseai.entity.TaskSubmitExtension;
import com.example.onlinecourseai.entity.TeacherNotification;
import com.example.onlinecourseai.mapper.AnswerRecordMapper;
import com.example.onlinecourseai.mapper.ClassInfoMapper;
import com.example.onlinecourseai.mapper.EnrollmentMapper;
import com.example.onlinecourseai.mapper.GradeMapper;
import com.example.onlinecourseai.mapper.QuestionMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.mapper.TaskRemindMapper;
import com.example.onlinecourseai.mapper.TaskStudyMapper;
import com.example.onlinecourseai.mapper.TaskSubmitExtensionMapper;
import com.example.onlinecourseai.mapper.TeacherNotificationMapper;
import com.example.onlinecourseai.service.AnswerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 问答模块业务：学生提交作答（客观题自动判分）→ 教师查看批阅总览与逐题作答 → 教师批阅主观题
 */
@Service
public class AnswerServiceImpl implements AnswerService {

    private final TaskMapper taskMapper;
    private final ClassInfoMapper classInfoMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final QuestionMapper questionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final GradeMapper gradeMapper;
    private final TaskSubmitExtensionMapper extensionMapper;
    private final TaskRemindMapper remindMapper;
    private final TeacherNotificationMapper notifyMapper;
    private final TaskStudyMapper taskStudyMapper;

    public AnswerServiceImpl(TaskMapper taskMapper,
                             ClassInfoMapper classInfoMapper,
                             EnrollmentMapper enrollmentMapper,
                             QuestionMapper questionMapper,
                             AnswerRecordMapper answerRecordMapper,
                             GradeMapper gradeMapper,
                             TaskSubmitExtensionMapper extensionMapper,
                             TaskRemindMapper remindMapper,
                             TeacherNotificationMapper notifyMapper,
                             TaskStudyMapper taskStudyMapper) {
        this.taskMapper = taskMapper;
        this.classInfoMapper = classInfoMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.questionMapper = questionMapper;
        this.answerRecordMapper = answerRecordMapper;
        this.gradeMapper = gradeMapper;
        this.extensionMapper = extensionMapper;
        this.remindMapper = remindMapper;
        this.notifyMapper = notifyMapper;
        this.taskStudyMapper = taskStudyMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SubmitResultVO submitAnswers(Long studentId, Long taskId, List<AnswerItemDTO> answers) {
        // 1. 任务存在
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        // 1a. 任务已关闭或超过截止时间，禁止提交（教师可在已提交记录中继续批阅）
        if ("CLOSED".equals(task.getStatus())) {
            throw new BizException("任务已关闭，无法提交作答");
        }
        LocalDateTime now = LocalDateTime.now();
        if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
            // 全局已过期：仅当该生持有未过期的单独顺延窗口（教师打回时录入）时放行
            TaskSubmitExtension ext = extensionMapper.findByTaskAndStudent(taskId, studentId);
            if (ext == null || ext.getExtendDeadline() == null || now.isAfter(ext.getExtendDeadline())) {
                throw new BizException("已超过截止时间，无法提交作答");
            }
        }
        // 2. 学生在该任务班级中
        Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), studentId);
        if (enrollment == null) {
            throw new BizException("你不在该任务的班级中，无法作答");
        }
        // 3. 不能重复提交
        if (gradeMapper.findByTaskAndStudent(taskId, studentId) != null) {
            throw new BizException("该任务已提交，不能重复作答");
        }
        if (answers == null || answers.isEmpty()) {
            throw new BizException("作答内容不能为空");
        }
        // 3.1 清理历史异常提交留下的半份作答（没有成绩却存在作答记录）：
        //     否则重新提交会撞 (task_id,question_id,student_id) 唯一键，学生永久交不上
        if (!answerRecordMapper.findByTaskAndStudent(taskId, studentId).isEmpty()) {
            answerRecordMapper.deleteByTaskAndStudent(taskId, studentId);
        }

        // 5. 加载任务课件下的全部题目，校验题目归属
        List<Question> questions = questionMapper.findByAnimationId(task.getAnimationId());
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        // 6. 逐题判分入库（按 questionId 去重，避免重复记录撞唯一键）
        Map<Long, AnswerItemDTO> uniqueAnswers = new LinkedHashMap<>();
        for (AnswerItemDTO item : answers) {
            if (item == null || item.getQuestionId() == null) {
                throw new BizException("作答数据不完整，请刷新后重新提交");
            }
            uniqueAnswers.put(item.getQuestionId(), item);
        }
        if (uniqueAnswers.isEmpty()) {
            throw new BizException("作答内容不能为空");
        }

        int objectiveScore = 0;
        boolean hasSubjective = false;
        for (AnswerItemDTO item : uniqueAnswers.values()) {
            Question q = questionMap.get(item.getQuestionId());
            if (q == null) {
                throw new BizException("题目不存在，请重新提交");
            }
            String answerText = item.getAnswerText() == null ? "" : item.getAnswerText().trim();
            AnswerRecord record = new AnswerRecord();
            record.setTaskId(taskId);
            record.setQuestionId(q.getId());
            record.setStudentId(studentId);
            record.setAnswerText(answerText);
            record.setSubmitTime(now);

            if ("objective".equals(q.getQuestionType())) {
                boolean correct = q.getAnswer() != null
                        && q.getAnswer().equalsIgnoreCase(answerText);
                int score = q.getScore() == null ? 0 : q.getScore();
                record.setIsCorrect(correct ? 1 : 0);
                record.setScore(correct ? score : 0);
                if (correct) {
                    objectiveScore += score;
                }
            } else {
                // 主观题：等待教师批阅
                record.setIsCorrect(null);
                record.setScore(0);
                hasSubjective = true;
            }
            answerRecordMapper.insert(record);
        }

        // 7. 写入成绩汇总：有主观题待批阅，全客观题直接完成
        Grade grade = new Grade();
        grade.setTaskId(taskId);
        grade.setStudentId(studentId);
        grade.setTotalScore(objectiveScore);
        grade.setTeacherScore(0);
        grade.setStatus(hasSubjective ? "REVIEWING" : "GRADED");
        grade.setSubmitTime(now);
        gradeMapper.insert(grade);
        // 补交成功即清除单独延期记录，避免残留延期反复放行
        extensionMapper.deleteByTaskAndStudent(taskId, studentId);

        // 通知任务发布教师：有学生提交了作业（旧任务无发布者则跳过）
        // 通知属于附带动作，失败不应回滚学生已经完成的提交
        if (task.getTeacherId() != null) {
            try {
                TeacherNotification notify = new TeacherNotification();
                notify.setTeacherId(task.getTeacherId());
                notify.setType("SUBMIT");
                notify.setTitle("新作业提交");
                notify.setContent("学生提交了任务《" + task.getTitle() + "》"
                        + ("REVIEWING".equals(grade.getStatus()) ? "，等待主观题批阅" : "，已完成自动判分"));
                notify.setRefTaskId(taskId);
                notify.setRefStudentId(studentId);
                notifyMapper.insert(notify);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SubmitResultVO result = new SubmitResultVO();
        result.setObjectiveScore(objectiveScore);
        result.setTotalQuestions(questions.size());
        result.setAnsweredQuestions(uniqueAnswers.size());
        result.setStatus(grade.getStatus());
        return result;
    }

    @Override
    public List<ReviewStudentVO> getReviewOverview(Long teacherId, Long taskId) {
        checkTaskOwner(teacherId, taskId);
        return gradeMapper.findReviewOverview(taskId);
    }

    @Override
    public List<StudentAnswerVO> getStudentAnswers(Long teacherId, Long taskId, Long studentId) {
        checkTaskOwner(teacherId, taskId);

        List<AnswerRecord> records = answerRecordMapper.findByTaskAndStudent(taskId, studentId);
        if (records.isEmpty()) {
            throw new BizException("该学生还未提交作答");
        }

        Task task = taskMapper.findById(taskId);
        Map<Long, Question> questionMap = new HashMap<>();
        for (Question q : questionMapper.findByAnimationId(task.getAnimationId())) {
            questionMap.put(q.getId(), q);
        }

        List<StudentAnswerVO> result = new ArrayList<>();
        for (AnswerRecord r : records) {
            Question q = questionMap.get(r.getQuestionId());
            StudentAnswerVO vo = new StudentAnswerVO();
            vo.setQuestionId(r.getQuestionId());
            vo.setQuestionText(q != null ? q.getQuestionText() : "");
            vo.setOptions(q != null ? q.getOptions() : null);
            vo.setQuestionType(q != null ? q.getQuestionType() : "");
            vo.setStudentAnswer(r.getAnswerText());
            vo.setCorrectAnswer(q != null ? q.getAnswer() : "");
            vo.setIsCorrect(r.getIsCorrect());
            vo.setScore(r.getScore());
            vo.setQuestionScore(q != null ? q.getScore() : null);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Grade gradeStudent(Long teacherId, Long taskId, Long studentId, Integer teacherScore, String comment, List<AnswerItemDTO> answers) {
        checkTaskOwner(teacherId, taskId);
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }

        if (teacherScore == null || teacherScore < 0) {
            throw new BizException("主观题得分不能为空且不能为负数");
        }
        // 主观分封顶：不得超过本任务主观题满分之和（全客观题任务满分=0，只允许填0）
        int subjectiveMax = questionMapper.sumSubjectiveScoreByAnimationId(task.getAnimationId());
        if (teacherScore > subjectiveMax) {
            throw new BizException("主观题得分不能超过主观题满分 " + subjectiveMax + " 分");
        }
        Grade grade = gradeMapper.findByTaskAndStudent(taskId, studentId);
        if (grade == null) {
            throw new BizException("该学生还未提交，无法批阅");
        }

        // 教师修改答案回写：客观题按新选择重新判分，主观题仅回写作答文本
        if (answers != null && !answers.isEmpty()) {
            Map<Long, Question> questionMap = new HashMap<>();
            for (Question q : questionMapper.findByAnimationId(task.getAnimationId())) {
                questionMap.put(q.getId(), q);
            }
            List<AnswerRecord> records = answerRecordMapper.findByTaskAndStudent(taskId, studentId);
            for (AnswerItemDTO item : answers) {
                if (item == null || item.getQuestionId() == null) {
                    continue;
                }
                Question q = questionMap.get(item.getQuestionId());
                if (q == null) {
                    continue;
                }
                AnswerRecord record = records.stream()
                        .filter(r -> r.getQuestionId().equals(item.getQuestionId()))
                        .findFirst().orElse(null);
                if (record == null) {
                    continue;
                }
                record.setAnswerText(item.getAnswerText());
                if ("objective".equals(q.getQuestionType())) {
                    boolean correct = q.getAnswer() != null
                            && q.getAnswer().equalsIgnoreCase(item.getAnswerText() == null ? "" : item.getAnswerText().trim());
                    int score = q.getScore() == null ? 0 : q.getScore();
                    record.setIsCorrect(correct ? 1 : 0);
                    record.setScore(correct ? score : 0);
                } else {
                    record.setIsCorrect(null);
                    record.setScore(0);
                }
                answerRecordMapper.updateAnswer(record);
            }
        }

        // 客观题得分 = 已判分记录得分之和
        int objectiveScore = 0;
        for (AnswerRecord r : answerRecordMapper.findByTaskAndStudent(taskId, studentId)) {
            if (r.getIsCorrect() != null) {
                objectiveScore += r.getScore();
            }
        }

        grade.setTeacherScore(teacherScore);
        grade.setComment(comment);
        grade.setTotalScore(objectiveScore + teacherScore);
        grade.setStatus("GRADED");
        gradeMapper.update(grade);
        return grade;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectStudent(Long teacherId, Long taskId, Long studentId, LocalDateTime extendDeadline) {
        if (studentId == null) {
            throw new BizException("请选择要打回的学生");
        }
        checkTaskOwner(teacherId, taskId);
        Task task = taskMapper.findById(taskId);
        if ("CLOSED".equals(task.getStatus())) {
            throw new BizException("任务已关闭，无法打回重做");
        }
        Grade grade = gradeMapper.findByTaskAndStudent(taskId, studentId);
        if (grade == null) {
            throw new BizException("该学生还未提交，无需打回");
        }
        LocalDateTime now = LocalDateTime.now();
        if (extendDeadline != null) {
            validateExtendDeadline(task, extendDeadline);
        } else if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
            // 已截止任务打回但没给新窗口，学生将无法重新提交
            throw new BizException("任务已截止，打回时必须填写晚于当前时间的补交截止");
        }
        // 清空本次逐题作答与成绩汇总，学生可在顺延窗口内重新提交
        answerRecordMapper.deleteByTaskAndStudent(taskId, studentId);
        gradeMapper.deleteByTaskAndStudent(taskId, studentId);
        // 单独顺延：给了新截止则写入/更新，未给则清除旧延期记录
        if (extendDeadline != null) {
            TaskSubmitExtension old = extensionMapper.findByTaskAndStudent(taskId, studentId);
            if (old == null) {
                TaskSubmitExtension ext = new TaskSubmitExtension();
                ext.setTaskId(taskId);
                ext.setStudentId(studentId);
                ext.setExtendDeadline(extendDeadline);
                extensionMapper.insert(ext);
            } else {
                old.setExtendDeadline(extendDeadline);
                extensionMapper.updateDeadline(old);
            }
        } else {
            extensionMapper.deleteByTaskAndStudent(taskId, studentId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void extendDeadline(Long teacherId, Long taskId, Long studentId, LocalDateTime extendDeadline) {
        if (studentId == null) {
            throw new BizException("请选择要顺延的学生");
        }
        checkTaskOwner(teacherId, taskId);
        if (extendDeadline == null) {
            throw new BizException("请选择补交截止时间");
        }
        Task task = taskMapper.findById(taskId);
        if ("CLOSED".equals(task.getStatus())) {
            throw new BizException("任务已关闭，顺延也无法提交，请先开启任务");
        }
        validateExtendDeadline(task, extendDeadline);
        Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), studentId);
        if (enrollment == null) {
            throw new BizException("该学生不在本任务班级中");
        }
        // 未提交学生：仅写入/更新单独顺延窗口，不清空任何作答记录
        TaskSubmitExtension old = extensionMapper.findByTaskAndStudent(taskId, studentId);
        if (old == null) {
            TaskSubmitExtension ext = new TaskSubmitExtension();
            ext.setTaskId(taskId);
            ext.setStudentId(studentId);
            ext.setExtendDeadline(extendDeadline);
            extensionMapper.insert(ext);
        } else {
            old.setExtendDeadline(extendDeadline);
            extensionMapper.updateDeadline(old);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remindStudent(Long teacherId, Long taskId, Long studentId) {
        if (studentId == null) {
            throw new BizException("请选择要催交的学生");
        }
        checkTaskOwner(teacherId, taskId);
        Task task = taskMapper.findById(taskId);
        if ("CLOSED".equals(task.getStatus())) {
            throw new BizException("任务已关闭，无需催交");
        }
        if (!isStudentTaskOpen(task, studentId)) {
            throw new BizException("任务已截止，无需催交；如需补交请先单独顺延");
        }
        Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), studentId);
        if (enrollment == null) {
            throw new BizException("该学生不在本任务班级中");
        }
        if (gradeMapper.findByTaskAndStudent(taskId, studentId) != null) {
            throw new BizException("该学生已提交作业，无需催交");
        }
        String message = "老师催交，请尽快完成本次作业";
        LocalDateTime now = LocalDateTime.now();
        TaskRemind old = remindMapper.findLatestByTaskAndStudent(taskId, studentId);
        if (old != null) {
            // 同一任务同一学生只保留一条提醒，重复催交只刷新时间并重新置为未读
            old.setTeacherId(teacherId);
            old.setMessage(message);
            old.setCreatedAt(now);
            remindMapper.refresh(old);
            return;
        }
        TaskRemind remind = new TaskRemind();
        remind.setTaskId(taskId);
        remind.setStudentId(studentId);
        remind.setTeacherId(teacherId);
        remind.setMessage(message);
        remind.setCreatedAt(now);
        remindMapper.insert(remind);
    }

    /** 任务是否仍可提交：未关闭，且未过全局截止或该生持有未过期的单独顺延 */
    private boolean isStudentTaskOpen(Task task, Long studentId) {
        if ("CLOSED".equals(task.getStatus())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (task.getDeadline() == null || !now.isAfter(task.getDeadline())) {
            return true;
        }
        TaskSubmitExtension ext = extensionMapper.findByTaskAndStudent(task.getId(), studentId);
        return ext != null && ext.getExtendDeadline() != null
                && !now.isAfter(ext.getExtendDeadline());
    }

    /** 顺延时间必须晚于当前，且不能早于任务原截止时间（避免越顺延越早） */
    private void validateExtendDeadline(Task task, LocalDateTime extendDeadline) {
        LocalDateTime now = LocalDateTime.now();
        if (extendDeadline.isBefore(now) || extendDeadline.isEqual(now)) {
            throw new BizException("补交截止时间必须晚于当前时间");
        }
        if (task.getDeadline() != null && extendDeadline.isBefore(task.getDeadline())) {
            throw new BizException("补交截止时间不能早于任务原截止时间");
        }
    }

    @Override
    public boolean hasUnreadRemind(Long studentId, Long taskId) {
        return remindMapper.countUnreadByTaskAndStudent(taskId, studentId) > 0;
    }

    @Override
    public List<Long> listUnreadRemindTaskIds(Long studentId) {
        return remindMapper.selectUnreadTaskIds(studentId);
    }

    @Override
    public List<Map<String, Object>> listMyNotices(Long studentId) {
        List<TaskRemind> list = remindMapper.selectAllByStudent(studentId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TaskRemind r : list) {
            Task t = taskMapper.findById(r.getTaskId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("taskId", r.getTaskId());
            m.put("animationId", t != null ? t.getAnimationId() : null);
            m.put("taskTitle", (t != null && t.getTitle() != null && !t.getTitle().isBlank()) ? t.getTitle() : "未命名任务");
            m.put("message", (r.getMessage() != null && !r.getMessage().isBlank()) ? r.getMessage() : "老师催交，请尽快完成本次作业");
            m.put("readFlag", r.getReadFlag() != null && r.getReadFlag() == 1 ? 1 : 0);
            m.put("createdAt", r.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    @Override
    public void readReminds(Long studentId, Long taskId) {
        remindMapper.markReadByTaskAndStudent(taskId, studentId);
    }

    @Override
    public StudentTaskResultVO getMyResult(Long studentId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        Enrollment enrollment = enrollmentMapper.findByClassAndStudent(task.getClassId(), studentId);
        if (enrollment == null) {
            throw new BizException("你不在该任务的班级中，无法查看");
        }

        StudentTaskResultVO result = new StudentTaskResultVO();
        result.setAnswers(new ArrayList<>());

        TaskStudyRecord study = taskStudyMapper.find(taskId, studentId);
        if (study != null) {
            result.setStudySeconds(study.getStudySeconds());
            result.setFirstViewAt(study.getFirstViewAt());
        }

        Grade grade = gradeMapper.findByTaskAndStudent(taskId, studentId);
        if (grade == null) {
            // 未提交：不返回题目与任何答案，避免提前泄露参考答案
            result.setSubmitted(false);
            result.setObjectiveScore(0);
            result.setTeacherScore(0);
            result.setTotalScore(0);
            return result;
        }

        List<Question> questions = questionMapper.findByAnimationId(task.getAnimationId());
        List<AnswerRecord> records = answerRecordMapper.findByTaskAndStudent(taskId, studentId);
        Map<Long, AnswerRecord> recordMap = new HashMap<>();
        for (AnswerRecord r : records) {
            recordMap.put(r.getQuestionId(), r);
        }

        int objectiveScore = 0;
        for (Question q : questions) {
            AnswerRecord r = recordMap.get(q.getId());
            StudentAnswerVO vo = new StudentAnswerVO();
            vo.setQuestionId(q.getId());
            vo.setQuestionText(q.getQuestionText());
            vo.setOptions(q.getOptions());
            vo.setQuestionType(q.getQuestionType());
            vo.setQuestionScore(q.getScore());
            vo.setStudentAnswer(r != null ? r.getAnswerText() : null);
            String reference = q.getAnswer();
            if (reference != null && "subjective".equals(q.getQuestionType())) {
                reference = reference.replaceFirst("^参考答案\\s*[:：]\\s*", "");
            }
            vo.setCorrectAnswer(reference);
            vo.setIsCorrect(r != null ? r.getIsCorrect() : null);
            vo.setScore(r != null ? r.getScore() : 0);
            if (r != null && r.getIsCorrect() != null) {
                objectiveScore += r.getScore() == null ? 0 : r.getScore();
            }
            result.getAnswers().add(vo);
        }

        result.setSubmitted(true);
        result.setObjectiveScore(objectiveScore);
        result.setTeacherScore(grade.getTeacherScore());
        result.setTotalScore(grade.getTotalScore());
        result.setComment(grade.getComment());
        result.setStatus(grade.getStatus());
        result.setSubmitTime(grade.getSubmitTime());
        return result;
    }

    /** 校验任务属于当前教师 */
    private void checkTaskOwner(Long teacherId, Long taskId) {
        Task task = taskMapper.findById(taskId);
        if (task == null) {
            throw new BizException("任务不存在");
        }
        Long ownerId = classInfoMapper.findById(task.getClassId()).getTeacherId();
        if (!ownerId.equals(teacherId)) {
            throw new BizException("只能操作自己发布的任务");
        }
    }
}
