/**
 * FileName: PmsBaseAttrInfoMapper
 * Author:   #include
 * Date:     2019/12/10 22:54
 * Description:
 */
package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/**
 */
public interface PmsBaseAttrInfoMapper extends Mapper<PmsBaseAttrInfo> {

    /**
     * 通过被检索商品的平台属性值的id和平台属性表进行联合查询
     * @param valueIdStr
     * @return
     */
    List<PmsBaseAttrInfo> selectAttrValueByValueIdList(@Param("valueIdStr") String valueIdStr);
}
