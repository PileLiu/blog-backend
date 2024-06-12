package com.llpy.enums;

import lombok.Getter;

/**
 * 响应错误
 *
 * @author llpy
 * @date 2024/06/12
 */
@Getter
public enum ResponseError {

    /**
     * 未知错误
     */
    NOT_FOUND_ERROR(404001, "该资源不存在"),
    USER_NOT_EXIST(500103, "用户不存在"),
    USER_EXIST(500104, "用户名已存在"),
    LOGIN_CODE_ERROR(500105, "验证码错误"),
    USER_EMAIL_EXIST(500104, "该邮箱已被使用"),
    NOT_FIND_DATA(500106, "查找不到对应数据"),
    USER_PASS_ERROR(500107, "密码错误!"),
    UPLOAD_IMG_ERROR(500108, "图片上传失败"),
    USER_EMAIL_ERROR(500109, "邮件发送失败"),
    USER_EMAIL_CODE_ERROR(500109, "邮件验证码错误"),
    USER_EMAIL_REGEX_ERROR(500110, "邮件格式错误");

    private final int code;
    private final String message;

    ResponseError(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
