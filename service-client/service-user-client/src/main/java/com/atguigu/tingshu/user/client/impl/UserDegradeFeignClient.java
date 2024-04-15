package com.atguigu.tingshu.user.client.impl;


import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.client.UserFeignClient;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfoVo(Long userId) {
        log.error("[用户服务]远程调用用户服务getUserInfoVo服务降级");
        return null;
    }

    @Override
    public Result<Map<Long, Integer>> userIsPaidTrack(Long userId, Long albumId, List<Long> needCheckBuyStausTrackIdList) {
        log.error("[用户服务]远程调用用户服务userIsPaidTrack服务降级");
        return null;
    }
}
