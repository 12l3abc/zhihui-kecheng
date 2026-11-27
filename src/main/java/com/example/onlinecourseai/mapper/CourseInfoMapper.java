package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.CourseInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseInfoMapper {

    @Insert("INSERT INTO course(teacher_id, course_name, course_code, credit, semester, description, created_at) " +
            "VALUES(#{teacherId}, #{courseName}, #{courseCode}, #{credit}, #{semester}, #{description}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseInfo course);

    @Select("SELECT * FROM course WHERE id = #{id}")
    CourseInfo findById(Long id);

    @Select("SELECT * FROM course WHERE teacher_id = #{teacherId} ORDER BY created_at DESC")
    List<CourseInfo> findByTeacherId(Long teacherId);

    @Select("SELECT COUNT(*) FROM course WHERE teacher_id = #{teacherId} AND course_name = #{courseName}")
    int countByNameAndTeacher(Long teacherId, String courseName);

    /** 按课程名精确查找（智能归科复用已有课程，避免重复建课） */
    @Select("SELECT * FROM course WHERE teacher_id = #{teacherId} AND course_name = #{courseName} LIMIT 1")
    CourseInfo findByTeacherIdAndName(@Param("teacherId") Long teacherId, @Param("courseName") String courseName);

    @Update("UPDATE course SET course_name=#{courseName}, course_code=#{courseCode}, credit=#{credit}, " +
            "semester=#{semester}, description=#{description} WHERE id=#{id}")
    int update(CourseInfo course);

    @Delete("DELETE FROM course WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM course WHERE teacher_id = #{teacherId} " +
            "AND course_name LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<CourseInfo> searchByTeacher(Long teacherId, String keyword);
}
