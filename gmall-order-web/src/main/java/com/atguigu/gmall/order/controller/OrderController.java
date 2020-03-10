/**
 * FileName: OrderController
 * Author:   #include
 * Date:     2020/2/26 15:10
 * Description:
 */
package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Controller
public class OrderController {

    @Reference
    private CartService cartService;

    @Reference
    private UserService userService;

    @Reference
    private OrderService orderService;

    @Reference
    private SkuService skuService;

    //获取订单页面体检的表单数据
    @LoginRequired(loginSucess = true)
    @RequestMapping("submitOrder")
    public ModelAndView submitOrder(String receiveAddressId, String tradeCode, ModelMap modelMap, HttpServletRequest request, HttpServletResponse response) {

        UmsMember umsMember = new UmsMember();
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        umsMember.setId(memberId);
        umsMember.setNickname(nickname);
        modelMap.put("umsMember",umsMember);

        //检验交易码
        String success = orderService.checkTradeCode(memberId,tradeCode);
        if ("success".equals(success)){
            //校验通过，用户可以提交订单
            List<OmsOrderItem> omsOrderItems = new ArrayList<>();
            BigDecimal totalAmount = new BigDecimal("0");

            //封装订单对象
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(new BigDecimal("7"));
            omsOrder.setCreateTime(new Date());
            omsOrder.setDiscountAmount(null);
            //omsOrder.setFreightAmount(); 运费，支付后，在生成物流信息时
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("快点发货");
            String outTradeNo = "gmall";
            outTradeNo = outTradeNo + System.currentTimeMillis();// 将毫秒时间戳拼接到外部订单号
            SimpleDateFormat sdf = new SimpleDateFormat("YYYYMMDDHHmmss");
            outTradeNo = outTradeNo + sdf.format(new Date());// 将时间字符串拼接到外部订单号

            omsOrder.setOrderSn(outTradeNo);//外部订单号
            omsOrder.setPayAmount(totalAmount);
            omsOrder.setOrderType(new BigDecimal("1"));
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId,memberId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            // 当前日期加一天，一天后配送
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,1);
            Date time = c.getTime();
            omsOrder.setReceiveTime(time);
            omsOrder.setSourceType(new BigDecimal("0"));
            omsOrder.setStatus(new BigDecimal("0"));
            omsOrder.setOrderType(new BigDecimal("0"));
            //omsOrder.setTotalAmount(totalAmount);

            //根据用户的id查询购物车列表
            List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
            for (OmsCartItem omsCartItem : omsCartItems) {
                if ("1".equals(omsCartItem.getIsChecked())){
                    //没有一个购物车里面的商品选中之后就生成一个商品订单详情对象
                    OmsOrderItem omsOrderItem = new OmsOrderItem();

                    // 检价
                    boolean b = skuService.checkPrice(omsCartItem.getProductSkuId(),omsCartItem.getPrice());
                    if (b == false) {
                        modelMap.put("errMsg","商品["+omsCartItem.getProductName()+"]价格已发生变化，请在购物车页面重新进行结算");
                        ModelAndView mv = new ModelAndView("tradeFail");
                        return mv;
                    }

                    //封装订单详情对象
                    // 验库存,远程调用库存系统
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());

                    omsOrderItem.setOrderSn(outTradeNo);// 外部订单号，用来和其他系统进行交互，防止重复
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setProductSkuCode("111111111111");
                    omsOrderItem.setSp1(omsCartItem.getSp1());
                    omsOrderItem.setSp2(omsCartItem.getSp2());
                    omsOrderItem.setSp3(omsCartItem.getSp3());
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库对应的商品编号");// 在仓库中的skuId

                    omsOrderItems.add(omsOrderItem);

                    totalAmount = totalAmount.add(omsCartItem.getTotalPrice());//计算商品的总价格

                }
            }
            omsOrder.setTotalAmount(totalAmount);
            omsOrder.setOmsOrderItems(omsOrderItems);
            //封装订单信息完毕，将订单信息保存至数据库
            OmsOrder omsOrderFormDb = orderService.saveOrder(omsOrder);


            //重定向到支付系统
            ModelAndView mv = new ModelAndView("redirect:http://localhost:8088/index");
            mv.addObject("outTradeNo",outTradeNo);
            return mv;
        }else {
            //校验不通过,用户不能提交订单
            modelMap.put("errMsg","结算页面过期或已失效，请重新结算。");
            ModelAndView mv = new ModelAndView("tradeFail");
            return mv;
        }

    }


    @LoginRequired(loginSucess = true)
    @RequestMapping("toTrade")
    public String toTrade(ModelMap modelMap, HttpServletRequest request, HttpServletResponse response) {

        UmsMember umsMember = new UmsMember();
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        umsMember.setId(memberId);
        umsMember.setNickname(nickname);

        BigDecimal totalAmount = new BigDecimal("0");

        //购物车商品信息集合
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        //收货人地址信息
        List<UmsMemberReceiveAddress> umsMemberReceiveAddressList = userService.getReceiveAddressByMemberId(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        for (OmsCartItem omsCartItem : omsCartItems) {
            //每循环一个购物车对象，就封装一个商品详情到OmsOrderItem
            if ("1".equals(omsCartItem.getIsChecked())) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                omsOrderItem.setProductPrice(omsCartItem.getPrice());
                omsOrderItems.add(omsOrderItem);
                totalAmount = totalAmount.add(omsCartItem.getTotalPrice());
            }
        }

        modelMap.put("umsMember", umsMember);
        modelMap.put("orderDetailList", omsOrderItems);
        modelMap.put("userAddressList", umsMemberReceiveAddressList);
        modelMap.put("totalAmount", totalAmount);

        //生成校验码，将防止用户多次提交订单
        String traderCode = orderService.getTradeCode(memberId);
        modelMap.put("tradeCode",traderCode);


        return "trade";
    }

}
