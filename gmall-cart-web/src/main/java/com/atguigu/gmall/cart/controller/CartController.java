/**
 * FileName: CartController
 * Author:   #include
 * Date:     2020/2/15 15:04
 * Description:
 */
package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 购物车功能
 */

@Controller
public class CartController {

    @Reference
    private SkuService skuService;

    @Reference
    private CartService cartService;



    //修改商品的选中状态的方法
    @LoginRequired(loginSucess = false)
    @RequestMapping("checkCart")
    public String checkCart(String isChecked,String skuId,ModelMap map,HttpServletRequest request, HttpServletResponse response){
        String memberId = (String)request.getAttribute("memberId");

        //调用服务，修改状态
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isChecked);
        if (isChecked.equals("1")){
            omsCartItem.setDeleteStatus(new BigDecimal("1"));
        }else {
            omsCartItem.setDeleteStatus(new BigDecimal("0"));
        }
        cartService.chechCart(omsCartItem);

        //将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);

        //计算商品的总价格
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        map.put("totalAmount",totalAmount);
        map.put("cartList",omsCartItems);

        return "cartListInner";
    }

    //修改商品数量的控制方法
    @LoginRequired(loginSucess = false)
    @RequestMapping("checkQuantity")
    public String checkQuantity(String quantity, String skuId, ModelMap modelMap,HttpServletRequest request, HttpServletResponse response){

        String memberId = (String)request.getAttribute("memberId");
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));
        cartService.chechCart(omsCartItem);

        //将最新的数据从缓存中查出，渲染给内嵌页
        List<OmsCartItem> omsCartItems = cartService.cartList(memberId);
        //计算商品的总价格
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartList",omsCartItems);

        return "cartListInner";
    }



    @LoginRequired(loginSucess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");

        if (StringUtils.isNotEmpty(memberId)){



            //用户已经登陆，先查询浏览器里面的cookie信息，如果cookie里面的购物车信息不为空就先向MySQL数据库合并购物车信息，
                                     // 再查询购物车，同事吧cookie中的数据清空
            String cartListCookieIsRegister = CookieUtil.getCookieValue(request, "cartListCookie", true);
            //判断缓存中是否有数据
            if (StringUtils.isNotEmpty(cartListCookieIsRegister)){
                //用户已经登录，但是浏览器cookie里面还有购物车信息，就行向mysql数据库合并信息，再查出数据库
                List<OmsCartItem> omsCartItemIsCookies = JSON.parseArray(cartListCookieIsRegister,OmsCartItem.class);
                for (OmsCartItem omsCartItemIsCookie : omsCartItemIsCookies) {
                    //计算出总价格
                    omsCartItemIsCookie.setTotalPrice(omsCartItemIsCookie.getPrice().multiply(omsCartItemIsCookie.getQuantity()));
                }

                //合并购物车
                OmsCartItem omsCartItemCookie = new OmsCartItem();
                omsCartItemCookie.setMemberId(memberId);
                omsCartItemCookie.setMemberNickname(nickname);
                cartService.mergeToCart(omsCartItemCookie, omsCartItemIsCookies);

                //删除购物车的cookie信息
                CookieUtil.deleteCookie(request,response,"cartListCookie");

            }

            //浏览器里面的cookie信息为空，直接查询数据库的购物车信息
            omsCartItems = cartService.cartList(memberId);
        }else {
            //用户没有登录，查询浏览器的cookie数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            //判断缓存中是否有数据
            if (StringUtils.isNotEmpty(cartListCookie)){
                //将cookie中查询出来的字符串转换为对象
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                for (OmsCartItem omsCartItem : omsCartItems) {
                    omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                }
            }
        }


        UmsMember umsMember = new UmsMember();
        umsMember.setId(memberId);
        umsMember.setNickname(nickname);
        //将用户信息放入到页面中
        modelMap.put("umsMember",umsMember);

        //计算商品的总价格
        BigDecimal totalAmount = getTotalAmount(omsCartItems);
        modelMap.put("totalAmount",totalAmount);
        modelMap.put("cartList",omsCartItems);

        return "cartList";
    }

    //计算购物车页面商品的总价格的方法
    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {

        BigDecimal totalAmount = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();

            if ("1".equals(omsCartItem.getIsChecked())){
                totalAmount = totalAmount.add(totalPrice);
            }
        }
        return totalAmount;
    }

    @LoginRequired(loginSucess = false)
    @RequestMapping("addToCart")
    public String addToCart(ModelMap model, String skuId, int quantity, HttpServletRequest request, HttpServletResponse response){

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        //调用商品服务查询商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        //将商品信息封装成购物车信息
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(new BigDecimal(0));
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        omsCartItem.setProductSkuCode(UUID.randomUUID().toString());
        omsCartItem.setSp1(skuInfo.getSkuSaleAttrValueList().get(0).getSaleAttrValueName());
        omsCartItem.setSp2(skuInfo.getSkuSaleAttrValueList().get(1).getSaleAttrValueName());
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked("0");
        omsCartItem.setQuantity(new BigDecimal(quantity));

        //判断用户是否登录
        String memberId = (String)request.getAttribute("memberId");
        String nickname = (String)request.getAttribute("nickname");


        if (StringUtils.isEmpty(memberId)){
            //用户没有登录，将购物车信息放入cookie中

            //cookie里原有的购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);

            if (StringUtils.isEmpty(cartListCookie)){
                //cookie为空时，直接将创建的OmsCartItem对象方法list集合中
                omsCartItems.add(omsCartItem);
            }else {
                //cookie中的购物车信息不为空，就获得cookie中的购物车对象的集合信息，更新cookie中的信息
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);

                //判断添加的购物车数据在cookie中是否存在
                boolean exist = if_cart_exist(omsCartItems,omsCartItem);

                if (exist){
                    //之前在给购物车里添加过该sku，更新购物车添加的数量
                    for (OmsCartItem cartItem : omsCartItems) {
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));//商品数量相加
                            //cartItem.setPrice(cartItem.getPrice().add(omsCartItem.getPrice()));//商品的总价格相加
                        }
                    }

                }else {
                    //之前没有过该商品，在购物车中新增该商品的信息
                    omsCartItems.add(omsCartItem);
                }

            }


                CookieUtil.setCookie(request,response,"cartListCookie",
                    JSON.toJSONString(omsCartItems),60*60*72,true);
        }else {


            //用户已经登录，将用户信息方法MySQL数据库中
            OmsCartItem omsCartItemFromDb = cartService.ifCartExitByUsder(memberId,skuId);
            if (omsCartItemFromDb == null){
                //该用户没有添加过当前商品
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname(nickname);
                cartService.addCart(omsCartItem);

            }else {
                //该用户添加过当前商品
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);

            }

            //同步缓存
            cartService.flushCartCache(memberId);


        }
        model.put("skuInfo",skuInfo);
        model.put("skuNum",quantity);

        return "success";
    }

    private boolean if_cart_exist(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {

        boolean flag = false;
        for (OmsCartItem cartItem : omsCartItems) {
            String skuId = cartItem.getProductSkuId();
            if (skuId.equals(omsCartItem.getProductSkuId())){
                //该商品存在，该 flag 为true
                flag = true;
                break;
            }
        }

        return flag;
    }

}
