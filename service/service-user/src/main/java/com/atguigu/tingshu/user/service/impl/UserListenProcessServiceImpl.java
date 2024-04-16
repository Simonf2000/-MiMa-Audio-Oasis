package com.atguigu.tingshu.user.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.KafkaConstant;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.service.KafkaService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private KafkaService kafkaService;

	@Override
	public BigDecimal getTrackBreakSecond(Long userId, Long trackId) {
		//1.用户未登录，返回0
		if (userId == null) {
			return new BigDecimal("0.00");
		}
		//2.根据用户ID+声音ID查询播放进度记录
		//2.1 创建查询对象-封装查询条件
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
		query.with(Sort.by(Sort.Direction.DESC, "updateTime"));
		query.limit(1);
		//2.2 动态构建用户播放进度集合 形式：前缀_用户ID
		String collName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collName);
		if (userListenProcess != null) {
			return userListenProcess.getBreakSecond();
		}
		return new BigDecimal("0.00");
	}

	@Override
	public void updateListenProcess(Long userId, UserListenProcessVo userListenProcessVo) {
		//1.根据用户ID+专辑ID+声音ID查询播放进度记录
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("albumId").is(userListenProcessVo.getAlbumId()).and("trackId").is(userListenProcessVo.getTrackId()));
		query.limit(1);
		String collName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collName);
		//2.如果播放进度存在则更新（时长，更新时间）
		BigDecimal breakSeckond = userListenProcessVo.getBreakSecond().setScale(2, RoundingMode.HALF_UP);
		if (userListenProcess != null) {
			userListenProcess.setBreakSecond(breakSeckond);
			userListenProcess.setUpdateTime(new Date());
		} else {
			//3.如果播放进度不存在则新增
			userListenProcess = new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setBreakSecond(breakSeckond);
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
		}
		mongoTemplate.save(userListenProcess, collName);

		//3.TODO 更新声音已经专辑播放统计数量
		//3.1 避免规定时间内多次重复更新统计数值 采用redis的set k v [ex] [nx]命令实现
		//3.1.1 创建避免多次重复更新Key 形式：包含用户ID,专辑ID,声音ID
		String key = RedisConstant.USER_TRACK_REPEAT_STAT_PREFIX + userId + "_" + userListenProcessVo.getAlbumId() + "_" + userListenProcessVo.getTrackId();
		//3.1.2 计算key的过期时间 当日结束时间-当前系统时间
		long ttl = DateUtil.endOfDay(new Date()).getTime() - System.currentTimeMillis();
		//3.1.2 调用redis的set nx存入Key，写入数据失败说明在重复操作
		Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, null, ttl, TimeUnit.MILLISECONDS);
		//3.2 发送Kafka消息进行异步更新统计数值
		if (flag) {
			//3.1 构建更新声音统计消息VO对象
			TrackStatMqVo mqVo = new TrackStatMqVo();
			mqVo.setBusinessNo(IdUtil.randomUUID());
			mqVo.setAlbumId(userListenProcessVo.getAlbumId());
			mqVo.setTrackId(userListenProcessVo.getTrackId());
			mqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			mqVo.setCount(1);
			kafkaService.sendKafkaMessage(KafkaConstant.QUEUE_TRACK_STAT_UPDATE, JSON.toJSONString(mqVo));
		}
	}
}
