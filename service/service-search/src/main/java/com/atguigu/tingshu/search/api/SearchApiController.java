package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
    * @Description: 仅用于测试业务逻辑-将指定专辑上架到索引库
    * @Param: [albumId]
    * @return: com.atguigu.tingshu.common.result.Result
    * @Author: simonf
    * @Date: 2024/4/9
    */
    @Operation(summary = "仅用于测试业务逻辑-将指定专辑上架到索引库")
    @GetMapping("/albumInfo/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId) {
        searchService.upperAlbum(albumId);
        return Result.ok();
    }

    /**
     * 该接口仅用于测试-下架专辑-删除文档
     * @param albumId
     * @return
     */
    @Operation(summary = "该接口仅用于测试-下架专辑-删除文档")
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId){
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "站内检索专辑")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery){
        AlbumSearchResponseVo vo = searchService.search(albumIndexQuery);
        return Result.ok(vo);
    }

}

