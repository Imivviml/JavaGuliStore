package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;


public interface CartService {

    OmsCartItem ifCartExitByUsder(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem omsCartItemFromDb);

    void flushCartCache(String memberId);

    List<OmsCartItem> cartList(String memberId);

    void chechCart(OmsCartItem omsCartItem);

    void mergeToCart(OmsCartItem omsCartItemCookie, List<OmsCartItem> omsCartItemIsCookies);

    void delCart(String memberId);
}
