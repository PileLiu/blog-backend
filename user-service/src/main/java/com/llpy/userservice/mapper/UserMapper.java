package com.llpy.userservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.llpy.entity.MenuVo;
import com.llpy.userservice.entity.User;
import com.llpy.userservice.entity.dto.UserDto2;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    UserDto2 getUser(Long userId);


}
