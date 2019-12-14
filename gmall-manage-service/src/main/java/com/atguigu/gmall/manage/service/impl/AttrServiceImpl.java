/**
 * FileName: AttrServiceImpl
 * Author:   #include
 * Date:     2019/12/10 22:53
 * Description:
 */
package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsBaseAttrValueMapper;
import com.atguigu.gmall.service.AttrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 */
@Service
public class AttrServiceImpl implements AttrService {

    @Autowired
    private PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    private PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    /**
     * 查询页面请求的商品属性列表
     * @param catalog3Id
     * @return
     */
    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);
        return pmsBaseAttrInfos;
    }

    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {

        //判断pmsBaseAttrInfo中的主键是否为空
        String infoId = pmsBaseAttrInfo.getId();
        if (StringUtils.isEmpty(infoId)) {
            //主键为空,保存操作
            //保存属性
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);//直插入有值的,如果为null则不插入

            //保存属性值
            List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                //通过属性设置属性值的外键
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }

        }else {
            //主键不为空,修改操作
            Example example = new Example(PmsBaseAttrInfo.class);
            //根据PmsBaseAttrInfo的主键进行修改
            example.createCriteria().andEqualTo("id",pmsBaseAttrInfo.getId());
            pmsBaseAttrInfoMapper.updateByExampleSelective(pmsBaseAttrInfo,example);

            //修改每个的属性值

            //先按照PmsBaseAttrInfo的主键将所有的属性值进行删除,然后在重新插入
            PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
            pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);

            //删除后将每个的属性的属性值重新插入
            List<PmsBaseAttrValue> attrValueLists = pmsBaseAttrInfo.getAttrValueList();
            for (PmsBaseAttrValue pmsBaseAttrValue : attrValueLists) {
                pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
                pmsBaseAttrValueMapper.insertSelective(pmsBaseAttrValue);
            }

        }


        return "success";
    }

    /**
     * 获得平台属性值
     * @param attrId
     * @return
     */
    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
        return pmsBaseAttrValues;
    }
}
