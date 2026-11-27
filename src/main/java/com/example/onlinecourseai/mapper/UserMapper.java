package com.example.onlinecourseai.mapper;

import com.example.onlinecourseai.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户表 Mapper
 */
@Mapper
public interface UserMapper {

    /** 按用户名查询（注册查重、登录校验） */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser findByUsername(String username);

    /** 按ID查询用户 */
    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    SysUser findById(Long id);

    /** 教师添加学生：按学号/姓名/专业模糊搜索已注册学生（最多50条） */
    @Select("SELECT id, username, real_name, role, gender, grade, major, phone, email FROM sys_user WHERE role = 'student' " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR real_name LIKE CONCAT('%', #{keyword}, '%') OR major LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY username LIMIT 50")
    java.util.List<SysUser> searchStudents(@Param("keyword") String keyword);

    /** 新增用户，自动回填自增主键到 user.id */
    @Insert("INSERT INTO sys_user(username, password, role, real_name, gender, grade, major, phone, email) " +
            "VALUES(#{username}, #{password}, #{role}, #{realName}, #{gender}, #{grade}, #{major}, #{phone}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser user);

    /** 更新个人资料（昵称、手机号、邮箱），账号与角色不可改 */
    @Update("UPDATE sys_user SET real_name = #{realName}, phone = #{phone}, email = #{email} WHERE id = #{id}")
    int updateProfile(SysUser user);

    /** 更新登录密码（已加密后的值） */
    @Update("UPDATE sys_user SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
