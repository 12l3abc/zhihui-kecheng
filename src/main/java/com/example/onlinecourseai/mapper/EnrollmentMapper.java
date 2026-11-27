package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.dto.StudentVO;
import com.example.onlinecourseai.entity.Enrollment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 选课表 Mapper（学生-班级）
 */
@Mapper
public interface EnrollmentMapper {

    /** 查询某学生是否已加入某班级（用于重复加入校验） */
    @Select("SELECT * FROM enrollment WHERE class_id = #{classId} AND student_id = #{studentId}")
    Enrollment findByClassAndStudent(Long classId, Long studentId);

    @Insert("INSERT INTO enrollment(class_id, student_id, join_time) " +
            "VALUES(#{classId}, #{studentId}, #{joinTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Enrollment enrollment);

    /** 班级学生名单（含学生信息字段，按加入时间排序） */
    @Select("SELECT u.id, u.username, u.real_name, u.gender, u.grade, u.major, e.join_time " +
            "FROM enrollment e JOIN sys_user u ON e.student_id = u.id " +
            "WHERE e.class_id = #{classId} ORDER BY e.join_time")
    List<StudentVO> findStudentsByClassId(Long classId);

    /** 某班级的学生数量（删除班级前的安全校验） */
    @Select("SELECT COUNT(*) FROM enrollment WHERE class_id = #{classId}")
    int countByClassId(Long classId);

    /** 班级合并：删除源班级中已在目标班级的学生选课记录（冗余记录） */
    @Delete("DELETE FROM enrollment WHERE class_id = #{oldClassId} " +
            "AND student_id IN (" +
            "  SELECT student_id FROM (SELECT student_id FROM enrollment WHERE class_id = #{newClassId}) tmp" +
            ")")
    int deleteDuplicates(Long oldClassId, Long newClassId);

    /** 班级合并：把源班级剩余学生全部迁入目标班级 */
    @Update("UPDATE enrollment SET class_id = #{newClassId} WHERE class_id = #{oldClassId}")
    int moveAll(Long oldClassId, Long newClassId);

    /** 教师把学生转出班级（删除选课记录） */
    @Delete("DELETE FROM enrollment WHERE class_id = #{classId} AND student_id = #{studentId}")
    int deleteByClassAndStudent(Long classId, Long studentId);

    /** 转班：把学生从源班级迁入目标班级（保留原 join_time） */
    @Update("UPDATE enrollment SET class_id = #{targetClassId} WHERE class_id = #{srcClassId} AND student_id = #{studentId}")
    int moveStudent(@Param("srcClassId") Long srcClassId, @Param("studentId") Long studentId, @Param("targetClassId") Long targetClassId);
}
