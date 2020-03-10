/**
 * FileName: PaymentServiceImpl
 * Author:   #include
 * Date:     2020/3/3 10:26
 * Description:
 */
package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.mq.ActiveMQUtil;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Service
public class PaymentServiceImpl implements PaymentService {


    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Autowired
    private AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {

        paymentInfoMapper.insertSelective(paymentInfo);

    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {

        //进行幂等性检查，防止在数据库中更新两次
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);
        //进行检查，如果个更新过就不再更新数据库中的信息
        if (StringUtils.isNotEmpty(paymentInfoResult.getPaymentStatus()) && ("已支付".equals(paymentInfoResult.getPaymentStatus()))) {
            //已经更新过，不用进行更新
            return;

        } else {

            String orderSn = paymentInfo.getOrderSn();
            Example e = new Example(PaymentInfo.class);
            e.createCriteria().andEqualTo("orderSn", orderSn);

            //连接ActiveMq
            Connection connection = null;
            Session session = null;
            try {

                //获取activemq工厂,获取session
                connection = activeMQUtil.getConnectionFactory().createConnection();
                session = connection.createSession(true, Session.SESSION_TRANSACTED);


            } catch (JMSException e1) {
                e1.printStackTrace();
            }

            try {


                //支付成功之后，引起的系统服务-->订单服务的更新-->库存服务-->物流服务
                //调用mq发送支付成功的消息

                //创建消息队列，
                Queue queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(queue);

                //有两种消息的发送模式
                //TextMessage textMessage = new ActiveMQTextMessage();//字符串文本

                MapMessage mapMessage = new ActiveMQMapMessage();// hash结构

                //将消息包含的参数设置到MapMessage中
                mapMessage.setString("out_trade_no", paymentInfo.getOrderSn());

                //将消息发送出去
                paymentInfoMapper.updateByExampleSelective(paymentInfo, e);
                //订单信息更新完成，发送消息
                producer.send(mapMessage);

                //更新成功，提交消息
                session.commit();
            } catch (Exception ex) {
                //发现异常，消息回滚
                try {
                    session.rollback();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            } finally {
                //关闭连接
                try {
                    connection.close();
                } catch (JMSException e1) {
                    e1.printStackTrace();
                }
            }
        }


    }

    //用来检查支付是否成功的延迟队列
    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int count) {

        //获取activemq的连接
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            //设置消息队列名称
            Queue payment_check_queue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(payment_check_queue);

            MapMessage mapMessage = new ActiveMQMapMessage();//hash结构

            //设置消息内容
            mapMessage.setString("out_trade_no", outTradeNo);//外部订单号
            mapMessage.setInt("count", count);//连续检查次数

            //为消息队列加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 30);

            producer.send(mapMessage);

            //提交消息
            session.commit();

        } catch (Exception e) {
            e.printStackTrace();
            //消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        } finally {
            //释放连接
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }

    }

    //调用支付宝的接口，检查订单的支付状态
    @Override
    public Map<String, Object> checkAlipayPayment(String out_trade_no) {

        Map<String,Object> resultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        request.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\"}");

        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response.isSuccess()){
            System.out.println("有可能交易已经创建成功，调用成功！");
            resultMap.put("out_trade_no",response.getOutTradeNo());
            resultMap.put("trade_no",response.getTradeNo());
            resultMap.put("trade_status",response.getTradeStatus());
            resultMap.put("call_back_content",response.getMsg());
        }else {
            System.out.println("有可能交易未创建，调用成功！");
        }

        return resultMap;
    }
}
