package com.example.onlinecourseai.common;

/**
 * 业务异常：用于可预期的业务错误（用户名重复、密码错误等），
 * 由 GlobalExceptionHandler 统一捕获并转成 Result 返回。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
