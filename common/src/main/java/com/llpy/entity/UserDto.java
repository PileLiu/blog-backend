package com.llpy.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)  //开启链式编程
@ApiModel(value="UserDto对象", description="用户Dto表")
public class UserDto implements Serializable {

    @ApiModelProperty(value = "用户id")
    private Integer UserId;

    @ApiModelProperty(value = "token认证")
    private String token;

}
