/**
 * FileName: UserMapper
 * Author:   #include
 * Date:     2019/12/1 17:08
 * Description:
 */
package com.atguigu.gmall.user.mapper;


import com.atguigu.gmall.user.bean.UmsMember;

import java.util.List;

/**
 */

public interface UserMapper {

    List<UmsMember> selectAllUser();
}
