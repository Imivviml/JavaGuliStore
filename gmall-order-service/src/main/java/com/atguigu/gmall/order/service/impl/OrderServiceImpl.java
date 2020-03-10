/**
 * FileName: OrderServiceImpl
 * Author:   #include
 * Date:     2020/2/27 16:13
 * Description:
 */
package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private OmsOrderMapper omsOrderMapper;

    @Autowired
    private OmsOrderItemMapper omsOrderItemMapper;

    @Reference
    private CartService cartService;

    @Autowired
    private ActiveMQUtil activeMQUtil;


    //生成交易码
    @Override
    public String getTradeCode(String memberId) {

        Jedis jedis = null;
        String tradeCode = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:"+memberId+":tradeCode";
            tradeCode = UUID.randomUUID().toString();
            jedis.setex(tradeKey,60*15,tradeCode);
        } finally {
            jedis.close();
        }
        return tradeCode;
    }

    //校验交易码
    @Override
    public String checkTradeCode(String memberId,String tradeCode) {

        Jedis jedis = null;
        String success = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:"+memberId+":tradeCode";
            String tradeCodeFormCache = jedis.get(tradeKey);


            // 使用lua脚本在发现key的同时将key删除，防止并发订单攻击
            //对比防重删令牌
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long) jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));

            if (eval!=null&&eval!=0){
                //jedis.del(tradeKey);//删除掉校验码，防止用户多次提交
                success = "success";
            }else {
                success = "fail";
            }

        } finally {
            jedis.close();
        }

        return success;
    }

    //将订单信息保存至数据库
    @Override
    public OmsOrder saveOrder(OmsOrder omsOrder) {

        omsOrderMapper.insertSelective(omsOrder);
        String orderId = omsOrder.getId();

        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(orderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
        //根据购物车是否删除的状态删除购物车数据库的商品信息
        //cartService.delCart(omsOrder.getMemberId());

        return omsOrder;

    }

    //通过外部订单号获取订单总金额
    @Override
    public OmsOrder getOrderByOrderSn(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        OmsOrder order = omsOrderMapper.selectOne(omsOrder);

        OmsOrderItem omsOrderItem = new OmsOrderItem();
        omsOrderItem.setOrderId(order.getId());
        List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);

        order.setOmsOrderItems(omsOrderItems);

        return order;
    }

    //监听消息队列中的消息，将订单信息进行更新
    @Override
    public void updateOrder(OmsOrder omsOrder) {


        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        OmsOrder omsOrderUpdate = new OmsOrder();
        omsOrderUpdate.setStatus(new BigDecimal("1"));


        //连接ActiveMq
        Connection connection = null;
        Session session =null;
        try {

            //获取activemq工厂,获取session
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);


        } catch (JMSException e1) {
            e1.printStackTrace();
        }

        try {

            //创建消息队列，
            Queue queue = session.createQueue("ORDER_PAY_QUEUE");
            MessageProducer producer = session.createProducer(queue);

            TextMessage textMessage = new ActiveMQTextMessage();
            //MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

            //将发送的消息内容设置到文本信息中
            //查询订单对象，将订单对象转换为字符串，存入ORDER_PAY_QUEUE的消息队列中
            OmsOrder omsOrderParam = new OmsOrder();
            omsOrderParam.setOrderSn(omsOrder.getOrderSn());
            OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);

            OmsOrderItem omsOrderItemParam = new OmsOrderItem();
            omsOrderItemParam.setOrderId(omsOrderParam.getId());
            List<OmsOrderItem> omsOrderItemResponse = omsOrderItemMapper.select(omsOrderItemParam);

            omsOrderResponse.setOmsOrderItems(omsOrderItemResponse);
            textMessage.setText(JSON.toJSONString(omsOrderResponse));

            //将消息发送出去
            omsOrderMapper.updateByExampleSelective(omsOrderUpdate, example);
            //订单完成支付之后，发送消息队列，提供给库存服务使用
            producer.send(textMessage);

            //更新成功，提交消息
            session.commit();
        }catch (Exception ex){
            //发现异常，消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }finally {
            //关闭连接
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }

    }
}
