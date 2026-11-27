package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.CoursewareFeedback;
import com.example.onlinecourseai.entity.SysUser;
import com.example.onlinecourseai.entity.TeacherNotification;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.mapper.CoursewareFeedbackMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.mapper.TeacherNotificationMapper;
import com.example.onlinecourseai.mapper.UserMapper;
import com.example.onlinecourseai.service.CoursewareFeedbackService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课件评价与疑问服务实现
 */
@Service
public class CoursewareFeedbackServiceImpl implements CoursewareFeedbackService {

    private final CoursewareFeedbackMapper feedbackMapper;
    private final AnimationMapper animationMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final TeacherNotificationMapper notifyMapper;

    public CoursewareFeedbackServiceImpl(CoursewareFeedbackMapper feedbackMapper,
                                         AnimationMapper animationMapper,
                                         TaskMapper taskMapper,
                                         UserMapper userMapper,
                                         TeacherNotificationMapper notifyMapper) {
        this.feedbackMapper = feedbackMapper;
        this.animationMapper = animationMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
        this.notifyMapper = notifyMapper;
    }

    @Override
    public CoursewareFeedback submitFeedback(Long studentId, String studentName,
                                             Long animationId, String type, String content) {
        if (animationId == null) {
            throw new BizException("课件ID不能为空");
        }
        if (type == null || (!"evaluation".equals(type) && !"question".equals(type))) {
            throw new BizException("反馈类型必须是 evaluation 或 question");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("反馈内容不能为空");
        }
        if (content.length() > 1000) {
            throw new BizException("反馈内容不能超过1000字");
        }

        // 校验课件存在
        Animation animation = animationMapper.findById(animationId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        // 学生只能评价自己班级任务里实际能访问的课件
        if (taskMapper.countAccessibleByStudent(animationId, studentId) == 0) {
            throw new BizException("你还没有收到这份课件，无法提交反馈");
        }

        // 获取学生真实姓名（如果没传）
        String name = studentName;
        if (name == null || name.isEmpty()) {
            SysUser user = userMapper.findById(studentId);
            if (user != null) {
                name = user.getRealName() != null ? user.getRealName() : user.getUsername();
            }
        }

        CoursewareFeedback fb = new CoursewareFeedback();
        fb.setAnimationId(animationId);
        fb.setStudentId(studentId);
        fb.setStudentName(name);
        fb.setType(type);
        fb.setContent(content.trim());
        fb.setCreatedAt(LocalDateTime.now());

        feedbackMapper.insert(fb);

        // 通知课件作者：收到学生新反馈（课件无归属教师则跳过）
        if (animation.getTeacherId() != null) {
            TeacherNotification notify = new TeacherNotification();
            notify.setTeacherId(animation.getTeacherId());
            notify.setType("FEEDBACK");
            notify.setTitle("新课件" + ("question".equals(type) ? "疑问" : "评价"));
            notify.setContent((name == null ? "学生" : name) + " 对《" + animation.getTitle() + "》提交了反馈");
            notify.setRefStudentId(studentId);
            notify.setRefAnimationId(animationId);
            notifyMapper.insert(notify);
        }
        return fb;
    }

    @Override
    public List<CoursewareFeedback> getByAnimationId(Long animationId, Long viewerId, String role) {
        if (animationId == null) {
            throw new BizException("课件ID不能为空");
        }
        Animation animation = animationMapper.findById(animationId);
        if (animation == null) {
            throw new BizException("课件不存在");
        }
        if ("teacher".equals(role)) {
            if (animation.getTeacherId() == null || !animation.getTeacherId().equals(viewerId)) {
                throw new BizException("只能查看自己课件的学生反馈");
            }
        } else if ("student".equals(role)) {
            if (taskMapper.countAccessibleByStudent(animationId, viewerId) == 0) {
                throw new BizException("你还没有收到这份课件，无法查看反馈");
            }
        } else {
            throw new BizException("没有权限查看课件反馈");
        }
        return feedbackMapper.findByAnimationId(animationId);
    }

    @Override
    public List<CoursewareFeedback> getByStudentId(Long studentId) {
        return feedbackMapper.findByStudentId(studentId);
    }

    @Override
    public List<CoursewareFeedback> getByTeacherId(Long teacherId) {
        return feedbackMapper.findByTeacherId(teacherId);
    }

    @Override
    public CoursewareFeedback updateOwnFeedback(Long id, Long studentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BizException("反馈内容不能为空");
        }
        if (content.length() > 1000) {
            throw new BizException("反馈内容不能超过1000字");
        }
        CoursewareFeedback fb = feedbackMapper.findById(id);
        if (fb == null) {
            throw new BizException("反馈记录不存在");
        }
        if (!fb.getStudentId().equals(studentId)) {
            throw new BizException("只能修改自己提交的反馈");
        }
        feedbackMapper.updateContent(id, content.trim());
        fb.setContent(content.trim());
        return fb;
    }

    @Override
    public void deleteOwnFeedback(Long id, Long studentId) {
        CoursewareFeedback fb = feedbackMapper.findById(id);
        if (fb == null) {
            throw new BizException("反馈记录不存在");
        }
        if (!fb.getStudentId().equals(studentId)) {
            throw new BizException("只能删除自己提交的反馈");
        }
        feedbackMapper.deleteById(id);
    }
}
