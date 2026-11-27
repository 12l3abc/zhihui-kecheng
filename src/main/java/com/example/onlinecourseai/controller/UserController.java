package com.example.onlinecourseai.controller;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.common.Result;
import com.example.onlinecourseai.entity.SysUser;
import com.example.onlinecourseai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<SysUser> register(@RequestBody SysUser user) {
        return Result.success(userService.register(user));
    }

    /** 登录：校验所选身份与账号角色一致，成功后把 userId 和 role 写入 Session */
    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String password = body.get("password");
        String role = body.get("role");
        SysUser user = userService.login(username, password);
        // 身份校验：前端选择的登录身份必须与账号实际角色一致，避免身份混淆
        if (role != null && !role.isBlank() && !role.equals(user.getRole())) {
            throw new BizException("该账号不是" + ("teacher".equals(role) ? "教师" : "学生")
                    + "账号，请选择正确的登录身份");
        }
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("username", user.getUsername());
        return Result.success(user);
    }

    /** 退出登录 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success();
    }

    /** 读取当前登录用户（个人设置打开时以服务端数据为准） */
    @GetMapping("/me")
    public Result<SysUser> me(HttpSession session) {
        return Result.success(userService.getById(currentUserId(session)));
    }

    /** 保存个人资料：昵称、手机号、邮箱 */
    @PostMapping("/profile")
    public Result<SysUser> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        SysUser user = userService.updateProfile(
                currentUserId(session),
                body.get("realName"),
                body.get("phone"),
                body.get("email"));
        return Result.success(user);
    }

    /** 修改密码 */
    @PostMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpSession session) {
        userService.changePassword(
                currentUserId(session),
                body.get("oldPassword"),
                body.get("newPassword"));
        return Result.success();
    }

    private Long currentUserId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (id == null) {
            throw new BizException("未登录，请先登录");
        }
        return ((Number) id).longValue();
    }
}
