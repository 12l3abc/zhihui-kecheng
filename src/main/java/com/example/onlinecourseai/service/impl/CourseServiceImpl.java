package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.CourseAnalysisResult;
import com.example.onlinecourseai.entity.Animation;
import com.example.onlinecourseai.entity.CourseInfo;
import com.example.onlinecourseai.mapper.AnimationMapper;
import com.example.onlinecourseai.mapper.CourseInfoMapper;
import com.example.onlinecourseai.service.CourseService;
import com.example.onlinecourseai.service.CourseSubjectClassifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseInfoMapper courseMapper;
    private final AnimationMapper animationMapper;
    private final CourseSubjectClassifier classifier;

    public CourseServiceImpl(CourseInfoMapper courseMapper,
                             AnimationMapper animationMapper,
                             CourseSubjectClassifier classifier) {
        this.courseMapper = courseMapper;
        this.animationMapper = animationMapper;
        this.classifier = classifier;
    }

    @Override
    public CourseInfo createCourse(Long teacherId, Map<String, Object> body) {
        String name = str(body.get("courseName"));
        if (name.isEmpty()) throw new BizException("课程名称不能为空");
        if (courseMapper.countByNameAndTeacher(teacherId, name) > 0) {
            throw new BizException("您已有同名课程，请换个名称");
        }
        CourseInfo c = new CourseInfo();
        c.setTeacherId(teacherId);
        c.setCourseName(name);
        c.setCourseCode(str(body.get("courseCode")));
        c.setCredit(body.get("credit") == null ? 2 : Integer.valueOf(body.get("credit").toString()));
        c.setSemester(str(body.get("semester")));
        c.setDescription(str(body.get("description")));
        c.setCreatedAt(LocalDateTime.now());
        courseMapper.insert(c);
        c.setCoursewareCount(0);
        return c;
    }

    @Override
    public List<CourseInfo> listMyCourses(Long teacherId) {
        List<CourseInfo> list = courseMapper.findByTeacherId(teacherId);
        fillCoursewareCount(list);
        return list;
    }

    @Override
    public CourseInfo updateCourse(Long teacherId, Long courseId, Map<String, Object> body) {
        CourseInfo c = getOwnCourse(teacherId, courseId);
        String name = str(body.get("courseName"));
        if (name.isEmpty()) throw new BizException("课程名称不能为空");
        if (!c.getCourseName().equals(name) && courseMapper.countByNameAndTeacher(teacherId, name) > 0) {
            throw new BizException("您已有同名课程");
        }
        c.setCourseName(name);
        c.setCourseCode(str(body.get("courseCode")));
        c.setCredit(body.get("credit") == null ? c.getCredit() : Integer.valueOf(body.get("credit").toString()));
        c.setSemester(str(body.get("semester")));
        c.setDescription(str(body.get("description")));
        courseMapper.update(c);
        c.setCoursewareCount(animationMapper.countByCourseId(courseId));
        return c;
    }

    @Override
    public void deleteCourse(Long teacherId, Long courseId) {
        getOwnCourse(teacherId, courseId);
        // 只解散课程与课件的归属关系，课件本身保留，避免误删教学资源
        animationMapper.clearCourseId(courseId);
        courseMapper.deleteById(courseId);
    }

    @Override
    public List<CourseInfo> searchCourses(Long teacherId, String keyword) {
        List<CourseInfo> list;
        if (keyword == null || keyword.trim().isEmpty()) {
            list = courseMapper.findByTeacherId(teacherId);
        } else {
            list = courseMapper.searchByTeacher(teacherId, keyword.trim());
        }
        fillCoursewareCount(list);
        return list;
    }

    @Override
    public CourseAnalysisResult analyzeAndAssign(Long teacherId) {
        List<Animation> allCoursewares = animationMapper.findByTeacherId(teacherId);
        if (allCoursewares.isEmpty()) {
            throw new BizException("您还没有课件，先生成课件再做智能归科");
        }

        CourseAnalysisResult out = new CourseAnalysisResult();
        out.setAnalyzed(allCoursewares.size());

        // 只处理尚未归课的课件：已经手动移动过的课件保留教师选择，不再被智能归科拉回
        List<Animation> coursewares = new ArrayList<>();
        for (Animation a : allCoursewares) {
            if (a.getCourseId() == null) {
                coursewares.add(a);
            }
        }
        if (coursewares.isEmpty()) {
            return out;
        }

        // 1. 逐份识别科目，按科目聚合
        Map<String, List<Animation>> grouped = new LinkedHashMap<>();
        Map<Long, CourseSubjectClassifier.Result> results = new LinkedHashMap<>();
        for (Animation a : coursewares) {
            CourseSubjectClassifier.Result r =
                    classifier.classify(a.getTitle(), a.getKnowledgePoint(), a.getSummary());
            results.put(a.getId(), r);
            grouped.computeIfAbsent(r.getSubject(), k -> new ArrayList<>()).add(a);
        }

        // 2. 每科建课（同名课程复用），再回写课件归属
        int assigned = 0;
        int created = 0;
        for (Map.Entry<String, List<Animation>> entry : grouped.entrySet()) {
            String subject = entry.getKey();
            List<Animation> items = entry.getValue();

            CourseInfo course = courseMapper.findByTeacherIdAndName(teacherId, subject);
            boolean isNew = false;
            if (course == null) {
                course = new CourseInfo();
                course.setTeacherId(teacherId);
                course.setCourseName(subject);
                course.setCourseCode(classifier.codePrefix(subject) + "-01");
                course.setCredit(2);
                course.setSemester(currentSemester());
                course.setDescription("智能归科自动创建｜" + items.size() + " 个课件：" + topicPreview(items));
                course.setCreatedAt(LocalDateTime.now());
                courseMapper.insert(course);
                isNew = true;
                created++;
            }

            for (Animation a : items) {
                if (!course.getId().equals(a.getCourseId())) {
                    animationMapper.updateCourseId(a.getId(), course.getId());
                    assigned++;
                }
            }

            CourseAnalysisResult.Group g = new CourseAnalysisResult.Group();
            g.setCourseId(course.getId());
            g.setCourseName(course.getCourseName());
            g.setCourseCode(course.getCourseCode());
            g.setCreated(isNew);
            g.setCount(items.size());
            for (Animation a : items) {
                CourseAnalysisResult.Item it = new CourseAnalysisResult.Item();
                it.setCoursewareId(a.getId());
                it.setTitle(a.getTitle());
                CourseSubjectClassifier.Result r = results.get(a.getId());
                it.setSubject(r.getSubject());
                it.setScore(r.getScore());
                it.setHits(r.getHits());
                g.getItems().add(it);
            }
            out.getGroups().add(g);
        }

        out.setAssigned(assigned);
        out.setCreatedCourses(created);
        return out;
    }

    @Override
    public List<Animation> listCoursewares(Long teacherId, Long courseId) {
        getOwnCourse(teacherId, courseId);
        return animationMapper.findByCourseId(courseId);
    }

    @Override
    public void moveCourseware(Long teacherId, Long animationId, Long courseId) {
        Animation a = animationMapper.findById(animationId);
        if (a == null) throw new BizException("课件不存在");
        if (!teacherId.equals(a.getTeacherId())) throw new BizException("只能操作自己的课件");
        if (courseId != null) {
            // 目标课程必须属于当前教师；courseId 为空表示移出课程
            getOwnCourse(teacherId, courseId);
        }
        animationMapper.updateCourseId(animationId, courseId);
    }

    /* ---------------- 内部工具 ---------------- */

    private void fillCoursewareCount(List<CourseInfo> list) {
        for (CourseInfo c : list) {
            c.setCoursewareCount(animationMapper.countByCourseId(c.getId()));
        }
    }

    private String topicPreview(List<Animation> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size() && i < 4; i++) {
            if (i > 0) sb.append("、");
            sb.append(items.get(i).getTitle());
        }
        if (items.size() > 4) sb.append(" 等");
        return sb.toString();
    }

    /** 当前学期：9-1 月为第一学期，2-8 月为第二学期 */
    private String currentSemester() {
        LocalDate now = LocalDate.now();
        int y = now.getYear();
        int m = now.getMonthValue();
        if (m >= 9) return y + "-" + (y + 1) + "学年第一学期";
        if (m == 1) return (y - 1) + "-" + y + "学年第一学期";
        return (y - 1) + "-" + y + "学年第二学期";
    }

    private CourseInfo getOwnCourse(Long teacherId, Long courseId) {
        CourseInfo c = courseMapper.findById(courseId);
        if (c == null) throw new BizException("课程不存在");
        if (!c.getTeacherId().equals(teacherId)) throw new BizException("只能操作自己的课程");
        return c;
    }

    private String str(Object o) { return o == null ? "" : o.toString().trim(); }
}
