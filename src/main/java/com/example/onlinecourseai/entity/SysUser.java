package com.example.onlinecourseai.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 sys_user（教师/学生共用）
 */
@Data
public class SysUser {

    private Long id;
    private String username;

    /**
     * 密码只允许写入（注册/登录时接收请求），序列化输出时永远不返回给前端
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 角色：teacher 教师 / student 学生 */
    private String role;

    private String realName;
    private LocalDateTime createdAt;

    /** 学生信息：性别 */
    private String gender;

    /** 学生信息：年级，如 2023级 */
    private String grade;

    /** 学生信息：专业，如 软件工程 */
    private String major;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;
}
