/**
 * FileName: PaymentController
 * Author:   #include
 * Date:     2020/2/29 16:31
 * Description:
 */
package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;


    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("/alipay/callback/return")
    @LoginRequired(loginSucess = true)
    public String aliPayCallBackReturn(HttpServletRequest request) {

        //获取回调地址中的回调信息
        String sign = request.getParameter("sign");//支付宝的签名，系统用于验证信息的正确性和安全性
        String trade_no = request.getParameter("trade_no");//支付宝的交易凭证号
        String out_trade_no = request.getParameter("out_trade_no");//订单信息的外部订单号
        String trade_status = request.getParameter("trade_status");//订单的交易状态
        String total_amount = request.getParameter("total_amount");//订单的交易金额
        String subject = request.getParameter("subject");//订单的标题
        String call_back_content = request.getQueryString();//支付宝的回调请求字符串

        // 通过支付宝的paramsMap进行签名验证，2.0版本的接口将paramsMap参数去掉了，导致同步请求没法验签
        if (StringUtils.isNotEmpty(sign)){
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setAlipayTradeNo(trade_no);//支付宝的交易凭证号
            paymentInfo.setCallbackContent(call_back_content);//支付宝的回调请求字符串
            paymentInfo.setCallbackTime(new Date());
            //更新订单的信息
            paymentService.updatePayment(paymentInfo);
        }


        //此时订单已经完成，将数据库中的信息更新

        //支付成功之后，引起的系统服务-->订单服务的更新-->库存服务-->物流服务

        //调用mq发送支付成功的消息

        return "finish";
    }


    //支付宝支付的方法
    @RequestMapping("alipay/submit")
    @LoginRequired(loginSucess = true)
    @ResponseBody
    public String alipay(String outTradeNo, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        OmsOrder omsOrder = orderService.getOrderByOrderSn(outTradeNo);


        //获取一个支付宝的请求的客户端（他并不是一个链接，而是一个封装好的http请求）
        String form = null;
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request

        // 回调函数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        //生成随机Id
        String out_trade_no = omsOrder.getOrderSn();
        //String out_trade_no = UUID.randomUUID().toString();
        //付款金额，必填
        String total_amount = "0.01";
        //订单名称，必填
        String subject = omsOrder.getOmsOrderItems().get(0).getProductName();
        //商品描述，可空
        String body = omsOrder.getOmsOrderItems().get(0).getSp1();
        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); // 调用SDK生成表单
            System.out.println(form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //创建交易订单，存入到数据库中，生成并保存用户的支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());//支付创建时间
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(outTradeNo);
        paymentInfo.setPaymentStatus("未付款");
        paymentInfo.setSubject(subject);//订单标题
        paymentInfo.setTotalAmount(omsOrder.getTotalAmount());//订单总金额
        //保存支付信息到数据库
        paymentService.savePaymentInfo(paymentInfo);

        //向消息中间件发送一个检查支付状态（支付服务消费）的延迟队列，用来检查订单是否支付成功
        paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,5);


        //提交信息到支付宝
        return form;
    }


    //微信支付的方法
    @RequestMapping("mx/submit")
    @LoginRequired(loginSucess = true)
    @ResponseBody
    public String mx(String outTradeNo, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {
        return "暂不支持微信支付，请选择支付宝支付！";
    }

    //支付的方法
    @RequestMapping("index")
    @LoginRequired(loginSucess = true)
    public String index(String outTradeNo, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) {

        UmsMember umsMember = new UmsMember();
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        umsMember.setId(memberId);
        umsMember.setNickname(nickname);

        OmsOrder omsOrder = orderService.getOrderByOrderSn(outTradeNo);

        modelMap.put("umsMember", umsMember);
        modelMap.put("outTradeNo", outTradeNo);
        modelMap.put("totalAmount", omsOrder.getTotalAmount());


        return "index";
    }

}
