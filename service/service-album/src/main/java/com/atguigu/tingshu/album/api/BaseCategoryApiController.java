package com.atguigu.tingshu.album.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.BaseAttribute;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory3;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@Tag(name = "分类管理")
@RestController
@RequestMapping(value="/api/album")
@SuppressWarnings({"all"})
public class BaseCategoryApiController {

	@Autowired
	private BaseCategoryService baseCategoryService;


	/**
	 * 查询所有分类（1、2、3级分类）
	 * @return
	 */
	@Operation(summary = "查询所有分类（1、2、3级分类）")
	@GetMapping("/category/getBaseCategoryList")
	public Result<List<JSONObject>> getBaseCategoryList(){
		List<JSONObject> list = baseCategoryService.getBaseCategoryList();
		return Result.ok(list);
	}

	@Operation(summary = "根据一级分类Id获取分类关联标签名，表浅值 列表")
	@GetMapping("/category/findAttribute/{category1Id}")
	public Result<List<BaseAttribute>> getAttributeByCategory1Id(@PathVariable Long category1Id){
		List<BaseAttribute> list = baseCategoryService.getAttributeByCategory1Id(category1Id);
		return Result.ok(list);
	}

	@Operation(summary = "根据三级分类ID查询分类信息")
	@GetMapping("/category/getCategoryView/{category3Id}")
	public Result<BaseCategoryView> getCategoryView(@PathVariable Long category3Id){
		BaseCategoryView baseCategoryView = baseCategoryService.getCategoryView(category3Id);
		return Result.ok(baseCategoryView);
	}

	@Operation(summary = "根据1级分类ID查询该分类下前7个置顶3级分类列表")
	@GetMapping("/category/findTopBaseCategory3/{category1Id}")
	public Result<List<BaseCategory3>> getTopBaseCategory3(@PathVariable Long category1Id){
		List<BaseCategory3> list = baseCategoryService.getTop7Category3(category1Id);
		return Result.ok(list);
	}


	/**
	 * 查询1级分类下所有二级级三级分类
	 * @param category1Id 1级分类ID
	 * @return {categoryId:1,categoryName:"音乐",categoryChild:[{},{}]}
	 */
	@Operation(summary = "查询1级分类下所有二级级三级分类")
	@GetMapping("/category/getBaseCategoryList/{category1Id}")
	public Result<JSONObject> getBaseCategoryList(@PathVariable Long category1Id){
		JSONObject jsonObject = baseCategoryService.getBaseCategoryListByCategory1Id(category1Id);
		return Result.ok(jsonObject);
	}

	/**
	 * 查询所有的一级分类信息
	 * @return
	 */
	@Operation(summary = "查询所有的一级分类信息")
	@GetMapping("/category/findAllCategory1")
	public Result<List<BaseCategory1>> getAllCategory1(){
		List<BaseCategory1> list = baseCategoryService.list();
		return Result.ok(list);
	}

}

