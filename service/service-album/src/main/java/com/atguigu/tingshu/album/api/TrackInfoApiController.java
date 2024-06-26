package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.login.GuiGuLogin;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumTrackListVo;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    //@GuiGuLogin
    @Operation(summary = "将音频文件上传到腾讯云点播平台")
    @PostMapping("/trackInfo/uploadTrack")
    public Result<Map<String, String>> uploadTrack(@RequestParam("file") MultipartFile trackFile) {
        Map<String, String> map = trackInfoService.uploadTrack(trackFile);
        return Result.ok(map);
    }

    @GuiGuLogin
    @Operation(summary = "保存声音")
    @PostMapping("/trackInfo/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        //1.获取当前用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.调用业务逻辑新增声音
        trackInfoService.saveTrackInfo(userId, trackInfoVo);
        return Result.ok();
    }

    @GuiGuLogin
    @Operation(summary = "获取当前登录声音分页列表")
    @PostMapping("/trackInfo/findUserTrackPage/{page}/{limit}")
    public Result<Page<TrackListVo>> getUserTrackByPage(@PathVariable int page, @PathVariable int limit, @RequestBody TrackInfoQuery trackInfoQuery) {
        //1.获取用户ID
        Long userId = AuthContextHolder.getUserId();
        if (userId != null) {
            //2.封装分页查询对象
            userId = trackInfoQuery.getUserId();
        }
        //3.调用业务层进行分页
        Page<TrackListVo> PageInfo = new Page<>(page, limit);
        PageInfo = trackInfoService.getUserTrackByPage(PageInfo, trackInfoQuery);
        return Result.ok(PageInfo);
    }

    @Operation(summary = "根据声音ID删除声音")
    @DeleteMapping("/trackInfo/removeTrackInfo/{id}")
    public Result removeTrackInfo(@PathVariable Long id) {
        trackInfoService.removeTrackInfo(id);
        return Result.ok();
    }

    @Operation(summary = "根据声音ID查询声音信息")
    @GetMapping("/trackInfo/getTrackInfo/{id}")
    public Result<TrackInfo> getTrackInfo(@PathVariable Long id) {
        TrackInfo trackInfo = trackInfoService.getById(id);
        return Result.ok(trackInfo);
    }

    @Operation(summary = "更新声音信息")
    @PutMapping("/trackInfo/updateTrackInfo/{id}")
    public Result updateTrackInfo(@PathVariable Long id, @RequestBody TrackInfo trackInfo) {
        trackInfoService.updateTrackInfo(trackInfo);
        return Result.ok();
    }

    /**
     * 分页展示专辑下声音列表-动态显示付费标识
     * 根据用户登录状态、身份、专辑付费类型、购买情况综合判断付费标识
     *
     * @param albumId
     * @param page
     * @param limit
     * @return 返回每一页声音列表（动态判断付费标识）
     */
    @GuiGuLogin(require = false)
    @Operation(summary = "分页展示专辑下声音列表-动态显示付费标识")
    @GetMapping("/trackInfo/findAlbumTrackPage/{albumId}/{page}/{limit}")
    public Result<Page<AlbumTrackListVo>> getAlbumTrackPage(@PathVariable Long albumId, @PathVariable int page, @PathVariable int limit) {
        //1.尝试获取用户ID
        Long userId = AuthContextHolder.getUserId();
        //2.创建MP分页对象
        Page<AlbumTrackListVo> pageInfo = new Page<>(page, limit);
        //3.调用业务层查询声音分页列表
        pageInfo = trackInfoService.getAlbumTrackPage(pageInfo, userId, albumId);
        return Result.ok(pageInfo);

    }
}

