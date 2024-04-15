package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.model.user.UserInfo;
import com.atguigu.tingshu.model.user.UserPaidAlbum;
import com.atguigu.tingshu.model.user.UserPaidTrack;
import com.atguigu.tingshu.user.mapper.UserInfoMapper;
import com.atguigu.tingshu.user.mapper.UserPaidAlbumMapper;
import com.atguigu.tingshu.user.mapper.UserPaidTrackMapper;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

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

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);

        if (userInfo != null) {
            return BeanUtil.copyProperties(userInfo, UserInfoVo.class);
        }
        return null;
    }

    @Override
    public void updateUser(Long userId, UserInfoVo userInfoVo) {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        userInfoMapper.updateById(userInfo);
    }

    @Override
    public Map<Long, Integer> getCheckBuyStausTrackIdList(Long userId, Long albumId, List<Long> needCheckBuyStausTrackIdList) {
        Map<Long, Integer> map = new HashMap<>();
        //1.判断用户是否已购买专辑
        //1.1 根据用户ID+专辑ID查询专辑购买情况
        LambdaQueryWrapper<UserPaidAlbum> userPaidAlbumLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getUserId, userId);
        userPaidAlbumLambdaQueryWrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        userPaidAlbumLambdaQueryWrapper.last("limit 1");
        Long count = userPaidAlbumMapper.selectCount(userPaidAlbumLambdaQueryWrapper);
        //1.2 如果购买专辑，将提交所有声音购买情况设置为：1 返回
        if (count > 0) {
            for (Long trackId : needCheckBuyStausTrackIdList) {
                map.put(trackId, 1);
            }
            return map;
        }

        //2.判断用户购买声音情况
        //2.1 根据用户ID+专辑ID查询已购声音列表
        LambdaQueryWrapper<UserPaidTrack> userPaidTrackLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getUserId, userId);
        userPaidTrackLambdaQueryWrapper.eq(UserPaidTrack::getAlbumId, albumId);
        userPaidTrackLambdaQueryWrapper.select(UserPaidTrack::getTrackId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(userPaidTrackLambdaQueryWrapper);
        //2.2 如果不存在已购声音，将提交所有声音购买情况设置为：0 返回
        if (CollectionUtil.isEmpty(userPaidTrackList)) {
            for (Long trackId : needCheckBuyStausTrackIdList) {
                map.put(trackId, 0);
            }
            return map;
        }
        //2.3 如果存在已购声音，动态判断声音购买情况(遍历待检查声音购买情况ID列表，判断ID是否包含在已购声音ID列表中)
        List<Long> userPaidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        for (Long trackId : needCheckBuyStausTrackIdList) {
            if (userPaidTrackIdList.contains(trackId)) {
                map.put(trackId, 1);
            } else {
                map.put(trackId, 0);
            }
        }
        return map;
    }
}


