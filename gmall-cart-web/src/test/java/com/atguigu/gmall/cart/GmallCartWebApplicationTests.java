package com.atguigu.gmall.cart;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.service.CartService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallCartWebApplicationTests {

    @Reference
    private CartService cartService;

    @Test
    public void contextLoads() {

        List<OmsCartItem> omsCartItems = cartService.cartList("1");
        for (OmsCartItem omsCartItem : omsCartItems) {


            if (omsCartItem.getDeleteStatus().compareTo(new BigDecimal("1"))==0){
                System.out.println(omsCartItem);
            }

        }

    }



}
