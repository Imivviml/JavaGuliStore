package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsProductSaleAttrMapper  extends Mapper<PmsProductSaleAttr> {

    //自定义查询sku的销售属性，并且将该sku的的属性改为选中状态
    List<PmsProductSaleAttr> selectspuSaleAttrListCheckBySku(@Param("productId") String productId, @Param("skuId") String skuId);
}
