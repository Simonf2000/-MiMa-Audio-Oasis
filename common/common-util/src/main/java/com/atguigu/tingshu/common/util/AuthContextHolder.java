package com.atguigu.tingshu.common.util;

/**
 * 获取当前用户信息帮助类
 */
public class AuthContextHolder {

    private static ThreadLocal<Long> userId = new ThreadLocal<Long>();

    public static void setUserId(Long _userId) {
        userId.set(_userId);
    }

    public static Long getUserId() {
        //TODO 没有认证前暂时固定为1，后续改为动态
        //return userId.get();
        return 1L;
    }

    public static void removeUserId() {
        userId.remove();
    }

}
