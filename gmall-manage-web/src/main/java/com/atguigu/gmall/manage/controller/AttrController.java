/**
 * FileName: AttrController
 * Author:   #include
 * Date:     2019/12/10 22:37
 * Description:
 */
package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.service.AttrService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 */
@CrossOrigin
@Controller
public class AttrController {

    @Reference
    private AttrService attrService;

    /**
     * 查询页面请求的商品属性列表
     * @param catalog3Id
     * @return
     */
    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id){
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrService.attrInfoList(catalog3Id);
        return pmsBaseAttrInfos;
    }

    /**
     * 保存属性值的业务处理方法
     * @param pmsBaseAttrInfo
     * @return
     */
    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo){
        String success = attrService.saveAttrInfo(pmsBaseAttrInfo);
        return "success";
    }

    /**
     * 获得品台属性
     * @param attrId
     * @return
     */
    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<PmsBaseAttrValue> getAttrValueList(String attrId){
        List<PmsBaseAttrValue> pmsBaseAttrValues = attrService.getAttrValueList(attrId);
        return pmsBaseAttrValues;
    }

}
