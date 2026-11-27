package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.dto.StudentVO;
import com.example.onlinecourseai.entity.ClassInfo;
import com.example.onlinecourseai.entity.Enrollment;
import com.example.onlinecourseai.entity.SysUser;
import com.example.onlinecourseai.mapper.ClassInfoMapper;
import com.example.onlinecourseai.mapper.EnrollmentMapper;
import com.example.onlinecourseai.mapper.TaskMapper;
import com.example.onlinecourseai.mapper.UserMapper;
import com.example.onlinecourseai.service.ClassService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ClassServiceImpl implements ClassService {

    private final ClassInfoMapper classInfoMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TaskMapper taskMapper;
    private final UserMapper userMapper;

    public ClassServiceImpl(ClassInfoMapper classInfoMapper,
                            EnrollmentMapper enrollmentMapper,
                            TaskMapper taskMapper,
                            UserMapper userMapper) {
        this.classInfoMapper = classInfoMapper;
        this.enrollmentMapper = enrollmentMapper;
        this.taskMapper = taskMapper;
        this.userMapper = userMapper;
    }

    @Override
    public ClassInfo createClass(Long teacherId, String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new BizException("班级名称不能为空");
        }
        String name = className.trim();
        // 同名查重：同一教师下不允许创建同名班级
        if (classInfoMapper.countByNameAndTeacher(teacherId, name) > 0) {
            throw new BizException("班级名称已存在，请换一个名称");
        }
        ClassInfo c = new ClassInfo();
        c.setClassName(name);
        c.setTeacherId(teacherId);
        c.setCreatedAt(LocalDateTime.now());
        classInfoMapper.insert(c);
        return c;
    }

    @Override
    public List<ClassInfo> getMyClasses(Long userId, String role) {
        if ("teacher".equals(role)) {
            return classInfoMapper.findByTeacherId(userId);
        }
        return classInfoMapper.findByStudentId(userId);
    }

    @Override
    public ClassInfo joinClass(Long studentId, Long classId) {
        ClassInfo c = classInfoMapper.findById(classId);
        if (c == null) {
            throw new BizException("班级不存在");
        }
        Enrollment exist = enrollmentMapper.findByClassAndStudent(classId, studentId);
        if (exist != null) {
            throw new BizException("已经加入该班级");
        }
        Enrollment e = new Enrollment();
        e.setClassId(classId);
        e.setStudentId(studentId);
        e.setJoinTime(LocalDateTime.now());
        enrollmentMapper.insert(e);
        return c;
    }

    @Override
    public List<StudentVO> getClassStudents(Long teacherId, Long classId) {
        ClassInfo c = classInfoMapper.findById(classId);
        if (c == null) {
            throw new BizException("班级不存在");
        }
        if (!c.getTeacherId().equals(teacherId)) {
            throw new BizException("只能查看自己创建的班级");
        }
        return enrollmentMapper.findStudentsByClassId(classId);
    }

    @Override
    public ClassInfo updateClass(Long teacherId, Long classId, String className) {
        if (className == null || className.trim().isEmpty()) {
            throw new BizException("班级名称不能为空");
        }
        String name = className.trim();
        ClassInfo c = getOwnClass(teacherId, classId);
        // 改名查重：排除自身后，同教师下不能重名
        if (!c.getClassName().equals(name)
                && classInfoMapper.countByNameAndTeacher(teacherId, name) > 0) {
            throw new BizException("班级名称已存在，请换一个名称");
        }
        classInfoMapper.updateName(classId, name);
        c.setClassName(name);
        return c;
    }

    @Override
    public void deleteClass(Long teacherId, Long classId) {
        ClassInfo c = getOwnClass(teacherId, classId);
        int studentCount = enrollmentMapper.countByClassId(classId);
        int taskCount = taskMapper.countByClassId(classId);
        if (studentCount > 0 || taskCount > 0) {
            StringBuilder sb = new StringBuilder("该班级还有");
            if (studentCount > 0) {
                sb.append(studentCount).append(" 名学生");
            }
            if (taskCount > 0) {
                if (studentCount > 0) {
                    sb.append("、");
                }
                sb.append(taskCount).append(" 个教学任务");
            }
            sb.append("，无法删除。可先使用「合并班级」把学生和任务转移到其他班级，再删除。");
            throw new BizException(sb.toString());
        }
        classInfoMapper.deleteById(classId);
    }

    @Override
    public List<ClassInfo> searchClasses(Long teacherId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return classInfoMapper.findByTeacherId(teacherId);
        }
        return classInfoMapper.searchByTeacher(teacherId, keyword.trim());
    }

    @Override
    public List<ClassInfo> mergeClasses(Long teacherId, List<Long> sourceIds, Long targetId) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            throw new BizException("请选择要合并的班级");
        }
        if (targetId == null) {
            throw new BizException("请选择目标班级");
        }
        Set<Long> uniqueIds = new HashSet<>(sourceIds);
        uniqueIds.remove(targetId); // 源班级不能等于目标班级
        if (uniqueIds.isEmpty()) {
            throw new BizException("目标班级不能同时作为被合并班级");
        }
        // 校验目标班级归属
        ClassInfo target = getOwnClass(teacherId, targetId);
        // 校验源班级归属并逐个迁移
        List<ClassInfo> merged = new ArrayList<>();
        for (Long sourceId : uniqueIds) {
            ClassInfo source = getOwnClass(teacherId, sourceId);
            // 1. 学生迁入目标班级：先删除源班中已在目标班的冗余选课记录，再迁移剩余
            enrollmentMapper.deleteDuplicates(sourceId, targetId);
            enrollmentMapper.moveAll(sourceId, targetId);
            // 2. 任务迁移到目标班级
            taskMapper.moveTasks(sourceId, targetId);
            // 3. 删除源班级
            classInfoMapper.deleteById(sourceId);
            merged.add(source);
        }
        return merged;
    }

    /** 校验班级存在且属于该教师，返回班级 */
    private ClassInfo getOwnClass(Long teacherId, Long classId) {
        ClassInfo c = classInfoMapper.findById(classId);
        if (c == null) {
            throw new BizException("班级不存在");
        }
        if (!c.getTeacherId().equals(teacherId)) {
            throw new BizException("只能操作自己创建的班级");
        }
        return c;
    }

    @Override
    public ClassInfo addStudent(Long teacherId, Long classId, String studentUsername) {
        ClassInfo c = getOwnClass(teacherId, classId);
        if (studentUsername == null || studentUsername.trim().isEmpty()) {
            throw new BizException("请输入学生学号/用户名");
        }
        String username = studentUsername.trim();
        // 1. 学生账号必须已注册且为学生角色
        SysUser student = userMapper.findByUsername(username);
        if (student == null || !"student".equals(student.getRole())) {
            throw new BizException("未找到该学生账号（请确认学号正确且该学生已注册）");
        }
        // 2. 已在班级中则拒绝
        if (enrollmentMapper.findByClassAndStudent(classId, student.getId()) != null) {
            throw new BizException("该学生已在班级中");
        }
        // 3. 加入班级
        Enrollment e = new Enrollment();
        e.setClassId(classId);
        e.setStudentId(student.getId());
        e.setJoinTime(LocalDateTime.now());
        enrollmentMapper.insert(e);
        return c;
    }

    @Override
    public void removeStudent(Long teacherId, Long classId, Long studentId) {
        getOwnClass(teacherId, classId);
        if (studentId == null) {
            throw new BizException("参数错误：缺少学生");
        }
        if (enrollmentMapper.findByClassAndStudent(classId, studentId) == null) {
            throw new BizException("该学生不在此班级中");
        }
        enrollmentMapper.deleteByClassAndStudent(classId, studentId);
    }

    @Override
    public void moveStudent(Long teacherId, Long srcClassId, Long studentId, Long targetClassId) {
        if (studentId == null || targetClassId == null) {
            throw new BizException("参数错误：缺少学生或目标班级");
        }
        if (srcClassId.equals(targetClassId)) {
            throw new BizException("目标班级与当前班级相同，无需转班");
        }
        getOwnClass(teacherId, srcClassId);
        getOwnClass(teacherId, targetClassId);
        if (enrollmentMapper.findByClassAndStudent(srcClassId, studentId) == null) {
            throw new BizException("该学生不在此班级中");
        }
        if (enrollmentMapper.findByClassAndStudent(targetClassId, studentId) != null) {
            throw new BizException("该学生已在目标班级中");
        }
        enrollmentMapper.moveStudent(srcClassId, studentId, targetClassId);
    }

    @Override
    public java.util.List<SysUser> searchStudents(Long teacherId, String keyword) {
        return userMapper.searchStudents(keyword == null ? null : keyword.trim());
    }
}
