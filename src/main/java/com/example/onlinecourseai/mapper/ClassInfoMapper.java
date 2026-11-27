package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.ClassInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 班级表 Mapper
 */
@Mapper
public interface ClassInfoMapper {

    /** 新增班级，自动回填主键 */
    @Insert("INSERT INTO class_info(class_name, teacher_id, created_at) " +
            "VALUES(#{className}, #{teacherId}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ClassInfo classInfo);

    @Select("SELECT * FROM class_info WHERE id = #{id}")
    ClassInfo findById(Long id);

    /** 教师创建的班级列表（含学生数） */
    @Select("SELECT c.*, (SELECT COUNT(*) FROM enrollment e WHERE e.class_id = c.id) AS student_count " +
            "FROM class_info c WHERE c.teacher_id = #{teacherId} ORDER BY c.created_at DESC")
    List<ClassInfo> findByTeacherId(Long teacherId);

    /** 学生加入的班级列表（通过选课表关联） */
    @Select("SELECT c.* FROM class_info c " +
            "JOIN enrollment e ON c.id = e.class_id " +
            "WHERE e.student_id = #{studentId} ORDER BY e.join_time DESC")
    List<ClassInfo> findByStudentId(Long studentId);

    /** 同一教师下同名班级数量（新建 / 改名查重） */
    @Select("SELECT COUNT(*) FROM class_info WHERE teacher_id = #{teacherId} AND class_name = #{className}")
    int countByNameAndTeacher(Long teacherId, String className);

    /** 修改班级名称 */
    @Update("UPDATE class_info SET class_name = #{className} WHERE id = #{id}")
    int updateName(Long id, String className);

    @Delete("DELETE FROM class_info WHERE id = #{id}")
    int deleteById(Long id);

    /** 按名称模糊搜索教师自己的班级（含学生数） */
    @Select("SELECT c.*, (SELECT COUNT(*) FROM enrollment e WHERE e.class_id = c.id) AS student_count " +
            "FROM class_info c WHERE c.teacher_id = #{teacherId} " +
            "AND c.class_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY c.created_at DESC")
    List<ClassInfo> searchByTeacher(Long teacherId, String keyword);
}
