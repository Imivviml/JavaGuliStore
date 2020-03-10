/**
 * FileName: OrderServiceMqListener
 * Author:   #include
 * Date:     2020/3/7 15:33
 * Description:
 */
package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 */

@Component
public class OrderServiceMqListener {

    @Autowired
    private OrderService orderService;

    //监听工厂，监听订单发送过来的消息
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        String out_trade_no = mapMessage.getString("out_trade_no");//获取消息队列中的信息

        System.out.println(out_trade_no);

        //更新订单的状态
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(out_trade_no);

        orderService.updateOrder(omsOrder);

        System.out.println("success");

    }

}
