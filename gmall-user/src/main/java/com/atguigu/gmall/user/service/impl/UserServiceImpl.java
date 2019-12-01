/**
 * FileName: UserServiceImpl
 * Author:   #include
 * Date:     2019/12/1 17:06
 * Description:
 */
package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 */

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

}
