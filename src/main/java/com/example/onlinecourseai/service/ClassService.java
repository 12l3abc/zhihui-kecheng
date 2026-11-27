package com.example.onlinecourseai.service;

import com.example.onlinecourseai.dto.StudentVO;
import com.example.onlinecourseai.entity.ClassInfo;

import java.util.List;

public interface ClassService {

    /** 教师创建班级（同名查重） */
    ClassInfo createClass(Long teacherId, String className);

    /** 我的班级（教师：我创建的；学生：我加入的） */
    List<ClassInfo> getMyClasses(Long userId, String role);

    /** 学生加入班级 */
    ClassInfo joinClass(Long studentId, Long classId);

    /** 教师查看班级学生名单（只能看自己创建的班级） */
    List<StudentVO> getClassStudents(Long teacherId, Long classId);

    /** 教师修改班级名称（排除自身查重） */
    ClassInfo updateClass(Long teacherId, Long classId, String className);

    /** 教师删除班级（班级下有学生或任务时拒绝） */
    void deleteClass(Long teacherId, Long classId);

    /** 教师按名称模糊搜索自己的班级 */
    List<ClassInfo> searchClasses(Long teacherId, String keyword);

    /** 班级合并：把多个源班级的学生与任务并入目标班级，再删除源班级 */
    List<ClassInfo> mergeClasses(Long teacherId, List<Long> sourceIds, Long targetId);

    /** 教师把已注册学生添加进班级（按学号/用户名） */
    ClassInfo addStudent(Long teacherId, Long classId, String studentUsername);

    /** 教师把学生转出班级 */
    void removeStudent(Long teacherId, Long classId, Long studentId);

    /** 教师把学生从源班级转入目标班级（转班） */
    void moveStudent(Long teacherId, Long srcClassId, Long studentId, Long targetClassId);

    /** 教师添加学生：按学号/姓名/专业搜索已注册学生 */
    java.util.List<com.example.onlinecourseai.entity.SysUser> searchStudents(Long teacherId, String keyword);
}
