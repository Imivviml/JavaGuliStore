/**
 * FileName: SpuController
 * Author:   #include
 * Date:     2019/12/13 23:06
 * Description:
 */
package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsProductImage;
import com.atguigu.gmall.bean.PmsProductInfo;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.service.SpuService;
import com.atguigu.gmall.utils.PmsUploadUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 */
@Controller
@CrossOrigin
public class SpuController {

    @Reference
    private SpuService spuService;

    /**
     * 通过三级分类的id查询页面商品的spu的方法
     * @param catalog3Id
     * @return
     */
    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id){
        List<PmsProductInfo> pmsProductInfos =  spuService.spuList(catalog3Id);
        return pmsProductInfos;
    }


    /**
     * 商品属性的保存
     * @return
     */
    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo (@RequestBody PmsProductInfo pmsProductInfo){

        String str = spuService.saveSpuInfo(pmsProductInfo);

        return "success";
    }


    /**
     * 上传图片获取图片信息的方法
     * @return
     */
    @RequestMapping("fileUpload")
    @ResponseBody
    //MultipartFile: 将页面上传过来的图片或者视屏的二进制信息转换为 MultipartFile 对象,可以通过此对象获取图片的元信息
    public String fileUpload(@RequestParam("file")MultipartFile multipartFile){

        //将图片或者视屏上传到分布式文件储存服务器上

        //返回文件的储存路径,以便前台页面的预览
        String imgUrl = PmsUploadUtil.uploadImage(multipartFile);

        return imgUrl;
    }


    /**
     * 查询平台销售属性的集合
     */
    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId){

        List<PmsProductSaleAttr>  pmsProductSaleAttrs = spuService.spuSaleAttrList(spuId);

        return pmsProductSaleAttrs;
    }


    /**
     * 查询平台sku商品的图片
     */
    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId){

        List<PmsProductImage> pmsProductImages =  spuService.spuImageList(spuId);

        return pmsProductImages;
    }


}
