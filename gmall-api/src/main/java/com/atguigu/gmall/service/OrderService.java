package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {
    String getTradeCode(String memberId);

    String checkTradeCode(String memberId,String tradeCode);

    OmsOrder saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOrderSn(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);
}
