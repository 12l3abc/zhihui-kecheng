package com.example.onlinecourseai.service.impl;

import com.example.onlinecourseai.common.BizException;
import com.example.onlinecourseai.entity.SysUser;
import com.example.onlinecourseai.mapper.UserMapper;
import com.example.onlinecourseai.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public SysUser register(SysUser user) {
        // 1. 参数校验
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new BizException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().length() < 6) {
            throw new BizException("密码长度不能少于6位");
        }
        if (!"teacher".equals(user.getRole()) && !"student".equals(user.getRole())) {
            throw new BizException("角色只能为 teacher 或 student");
        }
        // 2. 用户名查重
        SysUser exist = userMapper.findByUsername(user.getUsername());
        if (exist != null) {
            throw new BizException("用户名已存在");
        }
        // 3. 密码加密后入库
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
        // 4. 清空密码再返回
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser login(String username, String password) {
        if (username == null || password == null) {
            throw new BizException("用户名和密码不能为空");
        }
        SysUser user = userMapper.findByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser getById(Long id) {
        if (id == null) {
            throw new BizException("未登录，请先登录");
        }
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new BizException("账号不存在或已被删除");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser updateProfile(Long id, String realName, String phone, String email) {
        SysUser user = getById(id);
        // 昵称：必填，去掉首尾空白后限制长度
        if (realName == null || realName.trim().isEmpty()) {
            throw new BizException("昵称不能为空");
        }
        String name = realName.trim();
        if (name.length() > 20) {
            throw new BizException("昵称不能超过 20 个字符");
        }
        // 手机号：选填，填了就校验格式
        String tel = phone == null ? "" : phone.trim();
        if (!tel.isEmpty() && !tel.matches("1[3-9]\\d{9}")) {
            throw new BizException("手机号格式不正确");
        }
        // 邮箱：选填，填了就校验格式
        String mail = email == null ? "" : email.trim();
        if (!mail.isEmpty() && !mail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new BizException("邮箱格式不正确");
        }
        user.setRealName(name);
        user.setPhone(tel.isEmpty() ? null : tel);
        user.setEmail(mail.isEmpty() ? null : mail);
        userMapper.updateProfile(user);
        return getById(id);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        if (oldPassword == null || oldPassword.isBlank()) {
            throw new BizException("请输入当前密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BizException("新密码长度不能少于6位");
        }
        SysUser user = userMapper.findById(id);
        if (user == null) {
            throw new BizException("账号不存在或已被删除");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("当前密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BizException("新密码不能与当前密码相同");
        }
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }
}
