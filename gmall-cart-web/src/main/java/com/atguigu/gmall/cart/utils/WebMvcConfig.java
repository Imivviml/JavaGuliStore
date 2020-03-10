/**
 * FileName: WebMvcConfig
 * Author:   #include
 * Date:     2020/2/15 16:19
 * Description:
 */
package com.atguigu.gmall.cart.utils;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;



/**
 * 解决SpringBoo不能直接访问templates文件夹下的静态网页
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter{

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {


        /*registry.addViewController("/success.html").setViewName("success");
        registry.addViewController("/cartList.html").setViewName("cartList");
        registry.addViewController("/One_JDshop.html").setViewName("JDshop");*/
    }

}
