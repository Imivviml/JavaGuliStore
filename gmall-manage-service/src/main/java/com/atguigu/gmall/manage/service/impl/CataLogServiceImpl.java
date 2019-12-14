/**
 * FileName: cataLogServiceImpl
 * Author:   #include
 * Date:     2019/12/9 20:21
 * Description:
 */
package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;
import com.atguigu.gmall.manage.mapper.PmsCataLog1Mapper;
import com.atguigu.gmall.manage.mapper.PmsCataLog2Mapper;
import com.atguigu.gmall.manage.mapper.PmsCataLog3Mapper;
import com.atguigu.gmall.service.CataLogService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 */
@Service
public class CataLogServiceImpl implements CataLogService {

    @Autowired
    private PmsCataLog1Mapper pmsCataLog1Mapper;

    @Autowired
    private PmsCataLog2Mapper pmsCataLog2Mapper;

    @Autowired
    private PmsCataLog3Mapper pmsCataLog3Mapper;

    @Override
    public List<PmsBaseCatalog1> getCatalog1() {
        return pmsCataLog1Mapper.selectAll();
    }

    @Override
    public List<PmsBaseCatalog2> getCatalog2(String catalog1Id) {
        PmsBaseCatalog2 pmsBaseCatalog2 = new PmsBaseCatalog2();
        pmsBaseCatalog2.setCatalog1Id(catalog1Id);//使用通用Mapper封装参数
        //根据封装参数对象的属性进行查询
        List<PmsBaseCatalog2> catalog2s = pmsCataLog2Mapper.select(pmsBaseCatalog2);
        return catalog2s;
    }

    @Override
    public List<PmsBaseCatalog3> getCatalog3(String catalog2Id) {
        PmsBaseCatalog3 pmsBaseCatalog3 = new PmsBaseCatalog3();
        pmsBaseCatalog3.setCatalog2Id(catalog2Id);
        List<PmsBaseCatalog3> catalog3s = pmsCataLog3Mapper.select(pmsBaseCatalog3);
        return catalog3s;
    }
}
