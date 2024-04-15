package com.atguigu.tingshu.user.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user")
@SuppressWarnings({"all"})
public class UserInfoApiController {

	@Autowired
	private UserInfoService userInfoService;

	@Operation(summary = "根据指定用户ID查询用户基本信息")
	@GetMapping("/userInfo/getUserInfoVo/{userId}")
	public Result<UserInfoVo> getUserInfoVo(@PathVariable Long userId){
		UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
		return Result.ok(userInfoVo);
	}

	/**
	 * 专辑详情中每页声音列表(非免费试听)，对提交专辑以及声音ID进行判断得出每个声音购买情况
	 *
	 * @param userId                       用户ID
	 * @param albumId                      专辑ID
	 * @param needCheckBuyStausTrackIdList 需要判断购买状态声音ID集合
	 * @return {20158:1,20159:1}
	 */
	@Operation(summary = "专辑详情中每页声音列表，对提交专辑以及声音ID进行判断得出每个声音购买情况")
	@PostMapping("/userInfo/userIsPaidTrack/{userId}/{albumId}")
	public Result<Map<Long, Integer>> userIsPaidTrack(
			@PathVariable Long userId,
			@PathVariable Long albumId,
			@RequestBody List<Long> needCheckBuyStausTrackIdList
	) {
		Map<Long, Integer> map = userInfoService.getCheckBuyStausTrackIdList(userId, albumId, needCheckBuyStausTrackIdList);
		return Result.ok(map);
	}
}

