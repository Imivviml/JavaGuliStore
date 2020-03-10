package com.atguigu.gmall.cart;

import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallCartServiceApplicationTests {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;


    @Test
    public void contextLoads() {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId("1");
        List<OmsCartItem> select = omsCartItemMapper.select(omsCartItem);
        System.out.println(select.size());

    }

}
