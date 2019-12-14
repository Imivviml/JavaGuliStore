/**
 * FileName: UserServiceImpl
 * Author:   #include
 * Date:     2019/12/1 17:06
 * Description:
 */
package com.atguigu.gmall.user.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;

import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 */
//暴露服务
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers = userMapper.selectAll(); //userMapper.selectAllUser();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

//        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
//        umsMemberReceiveAddress.setMemberId(memberId);//创建查询条件,对象中不为空的属性就为过滤条件
//        return umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        Example example = new Example(UmsMemberReceiveAddress.class);
        example.createCriteria().andEqualTo("memberId",memberId);//创建查询条件
        //执行查询
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(example);
        return umsMemberReceiveAddresses;

    }
}
