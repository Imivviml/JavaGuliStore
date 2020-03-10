package com.atguigu.gmall.config;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.utils.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //被拦截的代码

        //判断被拦截的请求的访问的方法的注解（是否是需要拦截的方法）
        HandlerMethod handlerMethod = (HandlerMethod) handler;//强转
        LoginRequired methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequired.class);


        //是否拦截
        if (methodAnnotation == null) {//判断被拦截的方法上是否有自定义的拦截注解
            return true;//没有@LoginRequired拦截注解，不进行拦截，放行
        }

        //用户携带的的认证信息
        String token = "";

        //浏览器的cookie中存在的用户登录信息
        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        //如果浏览器里面的登录信息不为空，就说明用户之前登录过
        if (StringUtils.isNotEmpty(oldToken)) {
            token = oldToken;
        }

        //用户访问某些方法的时候携带的token，为用户访问时在url地址中携带的token
        String newToken = request.getParameter("token");
        //如果URL路径中的token不为空，就说明用户刚刚在认证中心验证过，token为验证信息
        if (StringUtils.isNotEmpty(newToken)) {
            token = newToken;
        }

        boolean loginSuccess = methodAnnotation.loginSucess();//获取请求登录是否必须 成功

        //检验token中的信息是否为真，或者token是否过期
        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if (StringUtils.isNotEmpty(token)) {

            String ip = request.getHeader("x-forwarded-for");//通过Nginx转发的客户端ip
            if (StringUtils.isEmpty(ip)){
                ip = request.getRemoteAddr();//从客户端获取ip
                if (StringUtils.isEmpty(ip)){
                    ip = "127..0.0.1";
                }
            }
            String successJson = HttpclientUtil.doGet("http://localhost:8086/verify?token=" + token+"&currentIp="+ip);

            successMap = JSON.parseObject(successJson, Map.class);

            success = successMap.get("status");

        }

        //是否必须登录成功才能访问
        if (loginSuccess) {
            //必须登录成功才能访问
            if (!success.equals("success")){//没有验证通过，可能是token过期了
                //踢回访问页
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://localhost:8086/login?originUrl="+requestURL);
                return false;
            }else {
                System.out.println(request.getRequestURL());
                //token验证通过，将token中的信息方法到cookie中去
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname", successMap.get("nickname"));
                //验证通过，将原来浏览器中的token信息覆盖掉
                if (StringUtils.isNotEmpty(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
                }
            }

        } else {
            //用户没有登录成功，但是也能使用功能，但是必须验证
            //如果验证通过将token中的信息存放入浏览器中的cookie中
            if (success.equals("success")){
                //需要将tooken携带的信息写入到cookie中
                request.setAttribute("memberId",successMap.get("memberId"));
                request.setAttribute("nickname",successMap.get("nickname"));
                //验证通过，将原来浏览器中的token信息覆盖掉
                if (StringUtils.isNotEmpty(token)){
                    CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
                }
            }

        }


        return true;
    }
}