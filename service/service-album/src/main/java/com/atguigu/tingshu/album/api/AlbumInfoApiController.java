package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    @Operation(summary = "保存专辑")
    @PostMapping("/albumInfo/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        albumInfoService.saveAlbumInfo(userId, albumInfoVo);
        return Result.ok();
    }

    @Operation(summary = "查询当前登录用户发布专辑分页列表")
    @PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
    public Result<Page<AlbumListVo>> getUserAlbumPage(@PathVariable int page,
                                                      @PathVariable int limit,
                                                      @RequestBody AlbumInfoQuery albumInfoQuery) {
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        Page<AlbumListVo> pageInfo = new Page<>(page, limit);
        pageInfo = albumInfoService.getUserAlbumPage(albumInfoQuery, pageInfo);
        return Result.ok(pageInfo);
    }

    @Operation(summary = "根据专辑ID删除专辑")
    @DeleteMapping("/albumInfo/removeAlbumInfo/{id}")
    public Result removeAlbumInfo(@PathVariable Long id){
        albumInfoService.removeAlbumInfo(id);
        return Result.ok();
    }

    @Operation(summary = "根据专辑ID查询专辑信息（包含专辑标签列表）")
    @GetMapping("/albumInfo/getAlbumInfo/{id}")
    public Result<AlbumInfo> getAlbumInfo(@PathVariable Long id) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(id);
        return Result.ok(albumInfo);
    }
}

