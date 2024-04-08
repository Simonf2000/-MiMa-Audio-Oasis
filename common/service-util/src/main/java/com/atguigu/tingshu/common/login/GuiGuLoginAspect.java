package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: smionf
 * @Date: 2024/04/07/19:43
 * @Description:
 */
@Slf4j
@Aspect
@Component
public class GuiGuLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 环绕通知逻辑，作用到切入点上（所有使用@GuiguLogin方法）
     *
     * @param pjp 目标方法对象
     * @return 目标方法执行结果
     * @throws Throwable
     */
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(guiGuLogin)")
    public Object authAround(ProceedingJoinPoint pjp, GuiGuLogin guiGuLogin) throws Throwable {

        //todo 采用切入点对象获取方法上注解属性值
        //Signature signature = pjp.getSignature();
        //MethodSignature methodSignature = (MethodSignature) signature;
        //Method method = methodSignature.getMethod();
        //GuiGuLogin annotation = method.getAnnotation(GuiGuLogin.class);
        //boolean require = annotation.require();

        //一 前置通知
        log.info("----------前置通知.....");
        //1.获取请求对象HttpServletRquest
        //1.1 从请求上下文中获取RequestAttributes接口-从ThreadLocal<RequestAttributes>中获取到的
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        //1.2 将RequestAttributes接口转为实现类ServletRequestAttributes
        ServletRequestAttributes sra = (ServletRequestAttributes) requestAttributes;

        //1.3 获取请求对象
        HttpServletRequest request = sra.getRequest();

        //1.4 获取小程序端token令牌
        String token = request.getHeader("token");


        String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        UserInfoVo userInfoVo = (UserInfoVo) redisTemplate.opsForValue().get(loginKey);
        if (guiGuLogin.require() && userInfoVo == null) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        if (userInfoVo != null) {
            Long ttlHours = redisTemplate.getExpire(loginKey, TimeUnit.HOURS);
            if (ttlHours <= 12) {
                redisTemplate.expire(loginKey,RedisConstant.USER_LOGIN_KEY_TIMEOUT,TimeUnit.SECONDS);
            }
            AuthContextHolder.setUserId(userInfoVo.getId());
        }


        Object retVal;
        try {
            retVal = pjp.proceed();
        } finally {
            // 后置通知
            log.info("----------后置通知.....");
            //清理ThreadLocal中用户信息-避免内存泄漏
            AuthContextHolder.removeUserId();
        }

        return retVal;
    }
}
