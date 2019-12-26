/**
 * FileName: ItemController
 * Author:   #include
 * Date:     2019/12/18 17:06
 * Description:
 */
package com.atguigu.gmall.item.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsBaseSaleAttr;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 */

@Controller
public class ItemController {

    @Reference
    private SkuService skuService;

    @Reference
    private SpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap map){

        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);

        // sku 对象
        map.put("skuInfo",pmsSkuInfo);

        //销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs =  spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        map.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        //查询pmsSkuInfo 和 pmsSkuSaleAttrValue 表，每一对销售属性值关联出唯一的商品sku的hash显示的存放到页面上，
        // 方便页面切换销售属性的时候查找到对应的商品sku

        //存放属性值对的hash集合
        Map<String,String> skuSaleAttrHash = new HashMap<>();
        List<PmsSkuInfo>  pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());

        //将查询出来的结果封装的hash表
        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String k = "";
            String v = skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|"; // 245|248
            }

            //将 k 和 v 添加进 hash
            skuSaleAttrHash.put(k,v);
        }

        //将hash表的键值对直接转换成 josn 直接放在页面上
        String skuSaleAttrHashJsonString = JSON.toJSONString(skuSaleAttrHash);
        map.put("skuSaleAttrHashJsonStr",skuSaleAttrHashJsonString);

        return "item";
    }


}
