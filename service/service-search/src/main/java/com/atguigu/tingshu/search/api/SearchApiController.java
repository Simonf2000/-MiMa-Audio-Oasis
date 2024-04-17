package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


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
     *
     * @param albumId
     * @return
     */
    @Operation(summary = "该接口仅用于测试-下架专辑-删除文档")
    @GetMapping("/albumInfo/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId) {
        searchService.lowerAlbum(albumId);
        return Result.ok();
    }

    @Operation(summary = "站内检索专辑")
    @PostMapping("/albumInfo")
    public Result<AlbumSearchResponseVo> search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo vo = searchService.search(albumIndexQuery);
        return Result.ok(vo);
    }

    /**
     * 查询1级下置顶三级分类热门前6专辑
     *
     * @param category1Id
     * @return
     */
    @Operation(summary = "查询1级下置顶三级分类热门前6专辑")
    @GetMapping("/albumInfo/channel/{category1Id}")
    public Result<List<Map<String, Object>>> getTop6AlbumByCategory1(@PathVariable Long category1Id) {
        List<Map<String, Object>> list = searchService.getTop6AlbumByCategory1(category1Id);
        return Result.ok(list);
    }

    @Operation(summary = "根据用户已录入文本进行展示相关提示词列表，实现自动补全")
    @GetMapping("/albumInfo/completeSuggest/{keyword}")
    public Result<List<String>> completeSuggest(@PathVariable String keyword) {
        List<String> list = searchService.completeSuggest(keyword);
        return Result.ok(list);
    }

    @Operation(summary = "更新Redis缓存中不同分类下不同排序方式TOP10列表")
    @GetMapping("/albumInfo/updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking() {
        searchService.updateLatelyAlbumRanking();
        return Result.ok();
    }

    @Operation(summary = "获取某个下某种排序方式TOP10排行榜")
    @GetMapping("/albumInfo/findRankingList/{category1Id}/{dimension}")
    public Result<List<AlbumInfoIndex>> getRankingList(@PathVariable Long category1Id, @PathVariable String dimension){
        List<AlbumInfoIndex> list = searchService.getRankingList(category1Id, dimension);
        return Result.ok(list);
    }
}



