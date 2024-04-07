package com.atguigu.tingshu.common.login;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/07/19:31
 * @Description:
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GuiGuLogin {


    /**
     * 要求该注解修饰方法必须登录才能调用
     *
     * @return
     */
    boolean require() default true;

}