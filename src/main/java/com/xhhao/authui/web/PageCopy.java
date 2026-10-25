package com.xhhao.authui.web;

import java.util.Map;

/** Title and lead text for each authentication page id. */
public record PageCopy(String title, String lead) {
    private static final Map<String, PageCopy> COPY = Map.of(
        "login", new PageCopy("登录账号", "欢迎回来，登录后继续。"),
        "signup", new PageCopy("创建账号", "几秒钟创建一个新账号。"),
        "logout", new PageCopy("退出登录", "确认后将退出当前账号。"),
        "password-reset-send", new PageCopy("找回密码", "输入注册邮箱，我们会发送重置链接。"),
        "password-reset-reset", new PageCopy("设置新密码", "为账号设置一个新密码。"),
        "two-factor", new PageCopy("两步验证", "输入验证器应用中的 6 位验证码。"));

    private static final PageCopy FALLBACK = new PageCopy("账号", "");

    public static PageCopy of(String pageId) {
        return COPY.getOrDefault(pageId, FALLBACK);
    }
}
