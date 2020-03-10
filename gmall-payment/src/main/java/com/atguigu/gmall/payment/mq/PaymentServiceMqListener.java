/**
 * FileName: PaymentServiceMqListener
 * Author:   #include
 * Date:     2020/3/8 10:55
 * Description:
 */
package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 */
@Component
public class PaymentServiceMqListener {

    @Autowired
    private PaymentService paymentService;

    //监听检查支付是否成功的方法
    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {

        //获取消息队列中的外部订单号和延迟队列的发送次数
        String out_trade_no = mapMessage.getString("out_trade_no");
        //防止active中有没有消费的队列
        Integer count = 0;
        if (mapMessage.getString("count") != null){
            count = Integer.parseInt(mapMessage.getString("count")+"");
        }

        //调用支付宝的接口，检查订单的支付状态
        System.out.println("进行延迟检查，调用支付宝的接口");
        Map<String,Object> resultMap = paymentService.checkAlipayPayment(out_trade_no);
        if (resultMap!=null&&!resultMap.isEmpty()){
            //返回回来的订单支付状态
            String trade_status = (String)resultMap.get("trade_status");
            if (StringUtils.isNotEmpty(trade_status)&&("TRADE_SUCCESS".equals(trade_status))){
                //查询带订单支付成功，将数据库中的信息进行更新
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setAlipayTradeNo((String)resultMap.get("trade_no"));//支付宝的交易凭证号
                paymentInfo.setCallbackContent((String)resultMap.get("call_back_content"));
                paymentInfo.setCallbackTime(new Date());

                System.out.println("已完成支付，调用支付宝查询接口成功，修改支付信息和发送支付成功的队列");
                paymentService.updatePayment(paymentInfo);
                //修改成功，不在进行延迟检查
                return;
            }
        }

        //延迟队列没有检查到订单是否支付成功，重新发送订单支付的延迟检查
        if (count>0){
            System.out.println("没有支付成功，检查剩余次数为"+count+",继续发送延迟检查任务");
            count --;
            paymentService.sendDelayPaymentResultCheckQueue(out_trade_no,count);
        }else {
            System.out.println("检查次数用尽，结束检查");
        }

    }

}
