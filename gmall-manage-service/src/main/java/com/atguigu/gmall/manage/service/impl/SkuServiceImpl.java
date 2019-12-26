/**
 * FileName: SkuServiceImpl
 * Author:   #include
 * Date:     2019/12/16 23:18
 * Description:
 */
package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 */
@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    private PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    private PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    private PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    private PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        //向表中插入 pmsSkeInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();//获取每一个sku的id

        //插入平台属性关联
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        //插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }
        //插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }


    }

    /**
     * 从数据库中查询个相关的商品的详细信息
     * @param skuId
     * @return
     */
    public PmsSkuInfo getSkuByIdInMysql(String skuId) {
        //根据主键id查询sku商品的信息
        PmsSkuInfo skuInfo = new PmsSkuInfo();
        skuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectOne(skuInfo);

        //根据具体商品的主键id查询出图片信息列表
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(pmsSkuImages);

        //查询出具体商品的
        return pmsSkuInfo;
    }


    /**
     * 查询商品详情页面的商品详细信息的业务处理方法 item 页面  从redis缓存中查询相关的商品详情信息
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        //根据主键id查询sku商品的信息
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();

        //连接缓存
        Jedis jedis = redisUtil.getJedis();

        //查询缓存
        String skuKey = "sku:" + skuId + ":info";
        String skuJson = jedis.get(skuKey);
        //通过工具类将json字符串转换为对象
        if (StringUtils.isNotEmpty(skuJson)){//如果从缓存中查询的不为空，就将 json 转换成对象
            pmsSkuInfo = JSON.parseObject(skuJson,PmsSkuInfo.class);
        }else {
            //如果从缓存中查询的为空就从数据库中查询
            pmsSkuInfo = getSkuByIdInMysql(skuId);
            //如果从数据库中查询的不为空，就将该对象存入到缓存中
            if (pmsSkuInfo != null){
                //mysql查询结果存入到缓存中
                jedis.set("sku:" + skuId + ":info",JSON.toJSONString(pmsSkuInfo));
            }else {
                //数据库中不存在该sku
                //为了防止缓存穿透，null或者空字符也要设置给redis
                jedis.setex("sku:" + skuId + ":info",60*3,JSON.toJSONString(""));//null值三分钟后过期
            }
        }
        jedis.close();
        //查询出具体商品的
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {

        List<PmsSkuInfo> pmsSkuInfos =  pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);

        return pmsSkuInfos;
    }
}
