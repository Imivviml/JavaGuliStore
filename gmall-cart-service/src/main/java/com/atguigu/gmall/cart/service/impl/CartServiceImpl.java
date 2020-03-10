/**
 * FileName: CartServiceImpl
 * Author:   #include
 * Date:     2020/2/17 10:41
 * Description:
 */
package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.*;


/**
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private OmsCartItemMapper omsCartItemMapper;

    @Autowired
    private RedisUtil redisUtil;

    //更改购物车选中状态

    @Override
    public void chechCart(OmsCartItem omsCartItem) {
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId())
                .andEqualTo("productSkuId",omsCartItem.getProductSkuId());
        omsCartItemMapper.updateByExampleSelective(omsCartItem,e);
        //同步缓存
        flushCartCache(omsCartItem.getMemberId());
    }

    //合并购物车的方法
    @Override
    public void mergeToCart(OmsCartItem omsCartItemCookie, List<OmsCartItem> omsCartItemIsCookies) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(omsCartItemCookie.getMemberId());
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        boolean flag = false;
        for (OmsCartItem omsCartItemIsCookie : omsCartItemIsCookies) {
            flag = false;
            //通过循环比价数据库中是否有与浏览器cookie中的商品是相同的产品
            for (OmsCartItem cartItem : omsCartItems) {
                if (cartItem.getProductSkuId().equals(omsCartItemIsCookie.getProductSkuId())){
                    cartItem.setQuantity(cartItem.getQuantity().add(omsCartItemIsCookie.getQuantity()));
                    updateCart(cartItem);
                    flag = true;
                }
            }
            if (!flag){
                //没有在数据库的信息中找到与购物车的信息中相同的产品，就将产品插入到MySQL数据库中
                omsCartItemIsCookie.setMemberId(omsCartItemCookie.getMemberId());
                omsCartItemIsCookie.setMemberNickname(omsCartItemCookie.getMemberNickname());
                addCart(omsCartItemIsCookie);
            }
        }
        //刷新购物车的redis的缓存
        flushCartCache(omsCartItemCookie.getMemberId());

    }

    //根据购物车的删除状态信息删除掉购物车信息
    @Override
    public void delCart(String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        for (OmsCartItem cartItem : omsCartItems) {
            if (cartItem.getDeleteStatus().compareTo(new BigDecimal("1")) == 0) {
                omsCartItemMapper.delete(cartItem);
            }
        }
        //删除完成之后刷新缓存
        flushCartCache(memberId);
    }

    @Override
    public OmsCartItem ifCartExitByUsder(String memberId, String skuId) {

        OmsCartItem cartItem = null;

        //一个用户下的相同商品的信息只能有一条，如果有多条就说明数据库中的数据有缺陷，
        try{
           OmsCartItem omsCartItem = new OmsCartItem();
           omsCartItem.setMemberId(memberId);
           omsCartItem.setProductSkuId(skuId);
           cartItem = omsCartItemMapper.selectOne(omsCartItem);
        }catch (Exception e){
            e.printStackTrace();
        }
        return cartItem;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {

        //向数据库中插入数据是，用户的相关信息一定不能为空
        if (StringUtils.isNotEmpty(omsCartItem.getMemberId())){
            omsCartItemMapper.insertSelective(omsCartItem);//避免添加空值
        }
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        //增加商品数量之后，更新数据库
        Example  example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id",omsCartItemFromDb.getId());
        omsCartItemMapper.updateByExample(omsCartItemFromDb,example);
    }

    @Override
    public void flushCartCache(String memberId) {

        //同步缓存的操作，将数据同步到redis中
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);



        //同步到redis缓存中
        Jedis jedis = redisUtil.getJedis();

        Map<String,String> map = new HashMap<>();
        for (OmsCartItem cartItem : omsCartItems) {
            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
            map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
        }

        jedis.del("user:"+memberId+":cart");
        jedis.hmset("user:" + memberId + ":cart",map);

        jedis.close();

    }

    /**
     * 查询商品详情页面的商品详细信息的业务处理方法 item 页面  从redis缓存中查询相关的商品详情信息
     * @param memberId
     * @return
     */

    @Override
    public List<OmsCartItem> cartList(String memberId) {


        //根据用户MemberId查询商品的信息
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        //连接缓存
        Jedis jedis = redisUtil.getJedis();

        //查询缓存
        String cartKey = "user:" + memberId + ":cart";
        List<String> hvals = jedis.hvals(cartKey);
        //通过工具类将json字符串转换为对象
        if (hvals != null){//如果从缓存中查询的不为空，就将 json 转换成对象

            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }

        }else {
            //如果从缓存中查询的为空就从数据库mysql中查询
            //设置分布式锁
            String token = UUID.randomUUID().toString();
            String OK = jedis.set("user:" + memberId + ":lock", token, "nx", "px", 10*1000);//拿到锁的线程有十秒的过期时间
            if (StringUtils.isNotEmpty(OK)&&"OK".equals(OK)){


                //设置成功，在10秒的过期时间内可以访问数据库，缓解数据库的压力
                omsCartItems = cartListByMySql(memberId);

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //如果从数据库中查询的不为空，就将该对象存入到缓存中
                if (omsCartItems != null){
                    //mysql查询结果存入到缓存中

                    Map<String,String> map = new HashMap<>();
                    for (OmsCartItem cartItem : omsCartItems) {
                        map.put("user:" + memberId + ":cart", JSON.toJSONString(cartItem));
                    }

                    jedis.hmset("user:" + memberId + ":cart",map);


                }else {
                    //数据库中不存在该cart数据
                    //为了防止缓存穿透，null或者空字符也要设置给redis
                    jedis.setex("user:" + memberId + ":cart",60*3,JSON.toJSONString(""));//null值三分钟后过期
                }
                // 在访问MySQL后将redis的分布式锁给释放掉
                String lockToken = jedis.get("user:" + memberId + ":lock");
                //确认删除的锁是自己的cart锁
                if (StringUtils.isNotEmpty(lockToken)&&lockToken.equals(token)){

                    jedis.del("user:" + memberId + ":lock");
                }

            }else {
                //设置失败，自旋（该线程睡眠几秒之后重新尝试访问该方法）
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return cartList(memberId);
            }

        }
        jedis.close();
        //查询出具体商品的
        return omsCartItems;
    }





    //用户已经登录，购物车查询redis缓存中的数据
    private List<OmsCartItem> cartListByMySql(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);
        System.out.println(omsCartItems);
        return omsCartItems;
    }

}
