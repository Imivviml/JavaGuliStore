/**
 * FileName: UserServiceImpl
 * Author:   #include
 * Date:     2019/12/1 17:06
 * Description:
 */
package com.atguigu.gmall.user.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;

import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;


/**
 */
//暴露服务
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;


    //用户登录的方法
    @Override
    public UmsMember login(UmsMember umsMember) {

        Jedis jedis = null;
        try{
            //获取jedis连接
            jedis = redisUtil.getJedis();

            //jedis开启失败
            if (jedis != null){
                //从缓存中查询用户信息，
                String umsMemberStr = jedis.get("user:" + umsMember.getNickname() + umsMember.getPassword() + ":info");
                if (StringUtils.isNotEmpty(umsMemberStr)){
                    //缓存中存在用户的信息
                    UmsMember umsMemberForCache = JSON.parseObject(umsMemberStr, UmsMember.class);
                    return umsMemberForCache;
                }
            }

            //连接redis失败，缓存中没有用户的信息，开启数据库开始查询
            UmsMember umsMemberForDb = loginForDb(umsMember);
            if (umsMemberForDb!=null){
                //将用户信息放入到缓存中
                jedis.setex("user:" + umsMemberForDb.getNickname() + umsMemberForDb.getPassword()+":info",
                                60*60*24,JSON.toJSONString(umsMemberForDb));
            }
            return umsMemberForDb;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            jedis.close();
        }
        //表明没有该用户，返回null
        return null;
    }

    //将token放入redis中一份
    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();

        jedis.setex("user:"+memberId+":token",60*60*2,token);

        jedis.close();
    }

    //添加第三方平台的用户
    @Override
    public UmsMember addOtherUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }

    //判断第三方用户是否已经在数据库中存在
    @Override
    public UmsMember checkOtherUser(UmsMember umsCheck) {

        UmsMember umsMember = userMapper.selectOne(umsCheck);

        return umsMember;
    }

    //根据用户id查询用户地址信息
    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
        return umsMemberReceiveAddresses;
    }

    //根据用户的地址id查询地址信息
    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId,String memberId) {

        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();

        //如果receiveAddressId为空，就查询用户的地址信息，获取默认的地址信息
        if (receiveAddressId == null||receiveAddressId.length()==0){
            umsMemberReceiveAddress.setMemberId(memberId);
            List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
            for (UmsMemberReceiveAddress memberReceiveAddress : umsMemberReceiveAddresses) {
                if ("1".equals(memberReceiveAddress.getDefaultStatus())){
                    return memberReceiveAddress;
                }
            }
        }else {
            umsMemberReceiveAddress.setId(receiveAddressId);
            UmsMemberReceiveAddress receiveAddress = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
            return receiveAddress;
        }

        return null;

    }

    private UmsMember loginForDb(UmsMember umsMember) {

        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if (umsMembers!=null){
            return umsMembers.get(0);
        }

        return null;

    }
}
