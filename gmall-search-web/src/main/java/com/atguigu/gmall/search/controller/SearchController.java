/**
 * FileName: SearchController
 * Author:   #include
 * Date:     2020/2/11 12:04
 * Description:
 */
package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 *
 */
@Controller
public class SearchController {

    @Reference
    private SearchService searchService;

    @Reference
    private AttrService attrService;

    @LoginRequired(loginSucess = false)
    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {

        List<PmsSearchSkuInfo> pmsSearchSkuInfos = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfos);

        //抽取出检索结果商品属性值的Id的集合（去重复）
        Set<String> valueIds = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> skuAttrValueList = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                String valueId = pmsSkuAttrValue.getValueId();
                valueIds.add(valueId);
            }
        }
        //通过去掉重复之后的结果来查询出商品品台属性的值的集合
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.getAttrValueByValueIdList(valueIds);

        //对平台属性集合进行进一步的处理，去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {

            //面包屑功能
            List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
            //delValueIds不为空，当前请求中含有属性值id的字符串数组

            for (String delValueId : delValueIds) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();//使用迭代器进行判断检查

                //生成面包屑的参数
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));

                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String id = pmsBaseAttrValue.getId();
                        if (delValueId.equals(id)) {

                            //根据匹配的属性值id设置属性值的名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());

                            //删除该属性值所在的属性组
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);

            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbs);
        }

        modelMap.put("attrList", pmsBaseAttrInfos);

        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotEmpty(keyword)) {
            modelMap.put("keyword", keyword);
        }


        return "list";
    }

    //拼接面包屑请求参数


    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String valueId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] valueIdStrs = pmsSearchParam.getValueId();

        //通过逻辑判断凭借url中请求的字符串
        String urlParam = "";
        if (StringUtils.isNotEmpty(keyword)) {
            if (StringUtils.isNotEmpty(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotEmpty(catalog3Id)) {
            if (StringUtils.isNotEmpty(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (valueIdStrs != null) {
            for (String valueIdStr : valueIdStrs) {
                if (!valueIdStr.equals(valueId)) {
                    urlParam = urlParam + "&valueId=" + valueIdStr;
                }
            }
        }

        return urlParam;
    }


    //拼接当前请求的字符串
    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] valueIdStrs = pmsSearchParam.getValueId();

        //通过逻辑判断凭借url中请求的字符串
        String urlParam = "";
        if (StringUtils.isNotEmpty(keyword)) {
            if (StringUtils.isNotEmpty(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotEmpty(catalog3Id)) {
            if (StringUtils.isNotEmpty(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (valueIdStrs != null) {
            for (String valueIdStr : valueIdStrs) {
                urlParam = urlParam + "&valueId=" + valueIdStr;
            }
        }

        return urlParam;
    }

    @LoginRequired(loginSucess = false)
    @RequestMapping("index")
    public String index() {
        return "index";
    }

}
