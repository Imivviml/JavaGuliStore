/**
 * FileName: PassportController
 * Author:   #include
 * Date:     2020/2/20 16:18
 * Description:
 */
package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.JwtUtil;
import com.atguigu.gmall.utils.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 */
@Controller
public class PassportController {

    @Reference
    private UserService userService;

    @RequestMapping("vlogin")
    public String vlogin(HttpServletRequest request, HttpServletResponse response, String code){

        //授权码换取access_token
        // client_secret=146bb259e72ed564a3312b7a297c779d
        // client_id=745753804
        //使用http工具类在新浪品台换取access_token

        //https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        String url = "https://api.weibo.com/oauth2/access_token?";
        Map<String,String> paramMap = new HashMap<>();
        paramMap.put("client_id","745753804");
        paramMap.put("client_secret","146bb259e72ed564a3312b7a297c779d");
        paramMap.put("grant_type","authorization_code");
        paramMap.put("redirect_uri","http://127.0.0.1:8086/vlogin");
        paramMap.put("code",code);// 授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String accessTokenJson = HttpclientUtil.doPost(url,paramMap);

        Map<String,Object> accessMap = JSON.parseObject(accessTokenJson,Map.class);

        System.out.println(accessMap);
        //access_token换取微博用户信息
        String uid = (String) accessMap.get("uid");
        String accessToken = (String) accessMap.get("access_token");
        String showUserUrl = "https://api.weibo.com/2/users/show.json?access_token="+accessToken+"&uid="+uid;
        String userJson = HttpclientUtil.doGet(showUserUrl);
        Map userMap = JSON.parseObject(userJson, Map.class);

        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessToken(accessToken);
        umsMember.setAccessCode(code);
        umsMember.setSourceUid((String)userMap.get("idstr"));
        umsMember.setCity((String)userMap.get("location"));
        umsMember.setNickname((String)userMap.get("screen_name"));
        umsMember.setUsername((String)userMap.get("screen_name"));

        String g = "0";
        String gender = (String) userMap.get("gender");
        if (gender.equals("m")){
            g = "1";
        }else if (gender.equals("f")){
            g = "2";
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember umsMemberCheck = userService.checkOtherUser(umsCheck);

        if (umsMemberCheck == null){
            //数据库中没有数据
            umsMember = userService.addOtherUser(umsMember);
        }else {
            umsMember = umsMemberCheck;
        }

        //使用jwt制作token
        String token = getToken(umsMember,request);


        //将用户信息保存到数据库，用户类型设置为微博用户

        return "redirect:http://localhost:8084/index?token="+token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token,String currentIp){

        Map<String,String>  map = new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "2020gmall", currentIp);

        if (decode!=null){
            //解析token成功
            map.put("status","success");
            map.put("memberId",(String)decode.get("memberId"));
            map.put("nickname",(String)decode.get("nickname"));
        }else {
            //解析token失败
            map.put("status","fail");
        }

        return JSON.toJSONString(map);
    }


    @RequestMapping("register")
    @ResponseBody
    public String register(UmsMember umsMember, HttpServletRequest request){

        //

        //调用用户服务来根据用户的信息进行登录
        UmsMember umsMemberLogin = userService.login(umsMember);


        return getToken(umsMemberLogin,request);
    }

    private String getToken(UmsMember umsMemberLogin, HttpServletRequest request) {

        String token = "";

        if (umsMemberLogin != null){
            //用户登录成功

            //使用jwt制作token
            String memberId = umsMemberLogin.getId();//rpc导致主键返回策略失效，会让
            String nickname = umsMemberLogin.getNickname();
            Map<String,Object> userMap = new HashMap<>();
            userMap.put("memberId",memberId);
            userMap.put("nickname",nickname);

            String ip = request.getHeader("x-forwarded-for");//通过Nginx转发的客户端ip
            if (StringUtils.isEmpty(ip)){
                ip = request.getRemoteAddr();//从客户端获取ip
                if (StringUtils.isEmpty(ip)){
                    ip = "127..0.0.1";
                }
            }

            //按照设计的加密算法进行加密，放入token中
            token = JwtUtil.encode("2020gmall", userMap, ip);

            //将token放入redis缓存中一份
            userService.addUserToken(token,memberId);
        }else {
            //用户登录失败
            token = "fail";
        }

        return token;

    }

    @RequestMapping("login")
    public String index(String originUrl, ModelMap modelMap){

        modelMap.put("originUrl",originUrl);
        return "login";
    }

}
