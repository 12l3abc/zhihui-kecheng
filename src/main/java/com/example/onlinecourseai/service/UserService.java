package com.example.onlinecourseai.service;

import com.example.onlinecourseai.entity.SysUser;

public interface UserService {

    /** 注册：用户名查重 + 密码加密，成功返回注册后的用户（不含密码） */
    SysUser register(SysUser user);

    /** 登录：校验用户名密码，成功返回用户（不含密码） */
    SysUser login(String username, String password);

    /** 按 ID 读取用户，不存在时抛出业务异常 */
    SysUser getById(Long id);

    /** 更新个人资料：昵称、手机号、邮箱 */
    SysUser updateProfile(Long id, String realName, String phone, String email);

    /** 修改密码：校验原密码后再改为新密码 */
    void changePassword(Long id, String oldPassword, String newPassword);
}
