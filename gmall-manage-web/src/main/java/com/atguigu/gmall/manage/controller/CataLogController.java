/**
 * FileName: CatalogController
 * Author:   #include
 * Date:     2019/12/9 18:00
 * Description:
 */
package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.service.CataLogService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 */
@Controller
@CrossOrigin
public class CataLogController {

    @Reference
    private CataLogService cataLogService;


    /**
     * 后台管理系统
     * 向页面响应一个分类目录
     * @return
     */
    @RequestMapping("getCatalog1")
    @ResponseBody
    public List<PmsBaseCatalog1> getCatalog1(){
        List<PmsBaseCatalog1> catalog1s = cataLogService.getCatalog1();
        return catalog1s;
    }


    /**
     * 根据页面选择的一级目录关联查询出二级目录
     * @return
     */
    @RequestMapping("getCatalog2")
    @ResponseBody
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id){
        List<PmsBaseCatalog2> catalog2s = cataLogService.getCatalog2(catalog1Id);
        return catalog2s;
    }


    /**
     * 页面的三级目录的查询,根据二级目录的关联查询
     * @param catalog2Id
     * @return
     */
    @RequestMapping("getCatalog3")
    @ResponseBody
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id){
        List<PmsBaseCatalog3> catalog3s = cataLogService.getCatalog3(catalog2Id);
        return catalog3s;
    }

}
