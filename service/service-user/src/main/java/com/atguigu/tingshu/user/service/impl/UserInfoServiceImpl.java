package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private KafkaService kafkaService;

    @Override
    public Map<String, String> wxLogin(String code) {
        try {
            //1.获取当前登录微信账户唯一标识：wxOpenId
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            if (sessionInfo != null) {

                String openid = sessionInfo.getOpenid();

                //2.判断当前微信是否为初次登录小程序
                LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(UserInfo::getWxOpenId, openid);
                UserInfo userInfo = userInfoMapper.selectOne(queryWrapper);

                //3.初次登录，初始化用户记录以及账户记录（余额）
                if (userInfo == null) {
                    //3.1 保存用户记录-将微信用户openID关联自定义用户
                    userInfo = new UserInfo();
                    userInfo.setWxOpenId(openid);
                    userInfo.setNickname("听友" + IdUtil.getSnowflakeNextId());
                    userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
                    userInfo.setIsVip(0);
//                    userInfo.setVipExpireTime(null);
                    userInfoMapper.insert(userInfo);
                    //3.2 发送Kafka消息 消息：用户ID
                    kafkaService.sendKafkaMessage(KafkaConstant.QUEUE_USER_REGISTER, userInfo.getId().toString());
                }
                //4.产生登录令牌（自定义登陆态）基于用户信息跟token进行关联
                //4.1 生成自定义token令牌
                String token = IdUtil.randomUUID();
                //4.2 构建登录key 形式：user:login:token
                String loginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                //4.3 将token作为Key 将用户信息UserInfoVo作为Value写入Redis
                UserInfoVo userInfoVo = BeanUtil.copyProperties(userInfo, UserInfoVo.class);
                redisTemplate.opsForValue().set(loginKey, userInfoVo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
                //5.封装登录结果
                Map<String, String> map = new HashMap<>();
                map.put("token", token);
                return map;
            }
            return null;
        } catch (Exception e) {
            log.error("[用户服务]微信登录异常：{}", e);
            throw new GuiguException(500, "登录失败:" + e.getMessage());
        }

    }
}


