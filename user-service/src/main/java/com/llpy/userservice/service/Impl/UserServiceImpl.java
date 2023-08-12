package com.llpy.userservice.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.llpy.entity.UserDto;
import com.llpy.enums.RedisKeyEnum;
import com.llpy.model.CodeMsg;
import com.llpy.model.Result;
import com.llpy.userservice.entity.User;
import com.llpy.userservice.entity.dto.MailDto;
import com.llpy.userservice.entity.query.UserLoginQuery;
import com.llpy.userservice.entity.dto.UserDto2;
import com.llpy.userservice.entity.dto.UserRegister;
import com.llpy.userservice.mapper.UserMapper;
import com.llpy.userservice.service.UserService;
import com.llpy.userservice.utils.EmailUtil;
import com.llpy.utils.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    //用户接口
    private final UserMapper userMapper;

    //jwt工具类
    private final JwtTokenUtil jwtTokenUtil;

    //发邮箱工具类
    private final EmailUtil emailUtil;
    //redis工具类
    private final RedisUtil redisUtil;
    //阿里云oss工具类
    private final AliOSSUtils aliOSSUtils;
    //正则表达式工具类
    private final RegexUtils regexUtils;

    public UserServiceImpl(UserMapper userMapper, JwtTokenUtil jwtTokenUtil, EmailUtil emailUtil, RedisUtil redisUtil, AliOSSUtils aliOSSUtils, RegexUtils regexUtils) {
        this.userMapper = userMapper;
        this.jwtTokenUtil = jwtTokenUtil;
        this.emailUtil = emailUtil;
        this.redisUtil = redisUtil;
        this.aliOSSUtils = aliOSSUtils;
        this.regexUtils = regexUtils;
    }
    //默认图片
    private final String DEFAULT_USER_IMG = "https://llpy-blog.oss-cn-shenzhen.aliyuncs.com/userImg/2023-08/defaul.jpg";

    /**
     * 登录
     *
     * @param userLoginQuery 用户登录查询
     * @return {@link Result}
     */
    @Override
    public Result login(UserLoginQuery userLoginQuery) {
        //根据用户名查找用户
        User user =
                userMapper.selectOne(
                        Wrappers.<User>lambdaQuery()
                                .eq(User::getUsername, userLoginQuery.getUsername()));
        //如果为空，返回用户不存在
        if (user == null) {
            return new Result<>(CodeMsg.USER_NOT_EXIST);
        } else {
            //存在则验证密码
            // TODO: 2023/7/26 后面把密码都改成加密的
            String password1 = userLoginQuery.getPassword();  //未加密的密码
            String password2 = DigestUtil.sha256Digest(password1);  //加密的密码
            //密码验证不通过，然会密码错误信息
            if (!user.getPassword().equals(password2) && !user.getPassword().equals(password1)) {
                return new Result<>(CodeMsg.USER_PASS_ERROR);
            }
            //生成一个不带‘ - ‘的uuid，用来和jwt一起存进redis
            String redisUserKey = UUID.randomUUID().toString().replaceAll("-", "");

            //创建要返回的对象
            UserDto userDto = new UserDto();
            userDto.setUserId(user.getUserId()).setToken(redisUserKey);

            //转为json字符串
            String jsonStr = JsonUtil.beanToJson(userDto);

            //生成随机数  增强安全性
            final String randomKey = jwtTokenUtil.getRandomKey();
            //生成token
            final String token = jwtTokenUtil.generateToken(jsonStr, randomKey);

            //存储token至redis
            redisUtil.set(RedisKeyEnum.REDIS_KEY_USER_INFO.getKey() + redisUserKey,
                    token,
                    RedisKeyEnum.REDIS_KEY_USER_INFO.getExpireTime());

            return Result.success(userDto);
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户id
     * @return {@link Result}<{@link UserDto2}>
     */
    @Override
    public Result<UserDto2> getUser(Long userId) {

        UserDto2 user = userMapper.getUser(userId);

        //将密码设为空再返回
        user.setPassword(null);

        return Result.success(user);
    }

    /**
     * 注销
     *
     * @param loginUser 登录用户
     */
    @Override
    public Result logout(UserDto loginUser) {
        redisUtil.del(RedisKeyEnum.REDIS_KEY_USER_INFO.getKey()+loginUser.getToken());
        return Result.success();
    }



    /**
     * 发送电子邮件
     *
     * @param email 邮件dto
     * @return {@link Result}<{@link ?}>
     */
    @Override
    public Result<?> sendEmail(String email) {
        //根据邮箱查找用户
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getEmail, email));

        //如果存在，直接返回
        if(user!=null) return new Result<>(CodeMsg.USER_EMAIL_EXIST);

        //验证邮箱格式
        if (!regexUtils.isEmail(email)) return new Result(CodeMsg.USER_EMAIL_REGEX_ERROR);
        //创建邮箱对象
        MailDto mailDto = new MailDto();

        mailDto.setTo(email);
        //调用工具类发送验证码
        MailDto mail = emailUtil.sendMail(mailDto);

        //如果状态码不为ok，返回发送失败
        if(!mail.getStatus().equals("ok")) return new Result<>(CodeMsg.USER_EMAIL_ERROR);

        //生成一个不带‘ - ‘的uuid，用来和邮箱验证码一起存进redis
        String redisEmailKey = UUID.randomUUID().toString().replaceAll("-", "");
        //1.获得枚举的key加上uuid，2.获得验证码，3.获得设置的默认过期时间
        redisUtil.set(RedisKeyEnum.REDIS_KEY_EMAIL_CODE.getKey()+ redisEmailKey,
                mail.getText(),
                RedisKeyEnum.REDIS_KEY_EMAIL_CODE.getExpireTime());

        return new Result<>("已发送，5分钟内有效~",200,redisEmailKey);
    }

    /**
     * 注册
     *
     * @param userRegister 用户注册
     * @return {@link Result}
     */
    @Override
    public Result register(UserRegister userRegister, String emailToken) {
        //验证邮箱格式
        if(!regexUtils.isEmail(userRegister.getEmail())) return new Result(CodeMsg.USER_EMAIL_REGEX_ERROR);

        //验证邮箱验证码
        String captcha =(String) redisUtil.get(RedisKeyEnum.REDIS_KEY_EMAIL_CODE.getKey() + emailToken);

        //redis中没有验证码或者验证不通过时返回
        if(captcha ==null||!captcha.equals(userRegister.getCaptcha())){
            //验证码错误
            return new Result<>(CodeMsg.USER_EMAIL_CODE_ERROR);
        }
        //根据用户名查找用户
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, userRegister.getUsername()));
        //如果存在该用户，返回
        if(user!=null) return new Result<>(CodeMsg.USER_EXIST);

        String password = DigestUtil.sha256Digest(userRegister.getPassword());  //加密密码

        //创建用户对象
        User newUser = new User();
        newUser.setUserId(null);
        newUser.setUsername(userRegister.getUsername())
                .setPassword(password)
                .setEmail(userRegister.getEmail())
                .setGender("男")   //默认性别
                .setNickname(userRegister.getNickname())
                .setUserImg(DEFAULT_USER_IMG)  //给新用户添加默认图片
                .setCreateTime(LocalDateTime.now());
        //插入
        userMapper.insert(newUser);
        return Result.success("注册成功，可以登录了");
    }


    /**
     * 更新用户
     *
     * @param userDto2 用户dto2
     * @return {@link Result}
     */
    @Override
    public Result updateUser(UserDto2 userDto2) {
        //验证邮箱格式
        boolean email = regexUtils.isEmail(userDto2.getEmail());
        if (!email) return new Result(CodeMsg.USER_EMAIL_REGEX_ERROR);

        //设置新对象
        User user = new User();
        user.setUserId(userDto2.getUserId());

        //如果密码不为空，就代表是更新密码
        if(userDto2.getPassword()!=null){
            //验证密码是否相同
            User oldUser = userMapper.selectById(userDto2.getUserId());

            //如果密码相同，直接返回密码无变化
            if (oldUser.getPassword().equals(userDto2.getPassword())) {
                return Result.success("密码无变化，无需改变");
            }

            //更新密码
            user.setPassword(userDto2.getPassword());
        }else{
            //否则就是更新用户信息
            user.setNickname(userDto2.getNickname())
                    .setCity(userDto2.getCity())
                    .setEmail(userDto2.getEmail())
                    .setGender(userDto2.getGender());
        }
        user.setUpdateTime(LocalDateTime.now());
        //更新
        userMapper.updateById(user);

        return Result.success("修改成功");
    }

    /**
     * 更新用户img
     *
     * @param file   文件
     * @param userId 用户id
     * @return {@link Result}<{@link ?}>
     */
    @Override
    public Result<?> updateUserImg(MultipartFile file, Long userId) {
        //拿到当前用户信息
        UserDto2 oldUser = getUser(userId).getData();

        //如果不是默认照片，先进行删除操作
        if(oldUser.getUserImg()!=null && !oldUser.getUserImg().equals(DEFAULT_USER_IMG)){
            aliOSSUtils.delete(oldUser.getUserImg());
        }
        try {
            //上传
            String url = aliOSSUtils.upload(file);
            //更新时间
            LocalDateTime updateTime = LocalDateTime.now();
            //新建对象
            User user = new User();
            user.setUserId(userId)
                    .setUserImg(url)
                    .setUpdateTime(updateTime);
            //更新用户信息
            userMapper.updateById(user);
        } catch (IOException e) {
            return new Result<>(CodeMsg.UPLOAD_IMG_ERROR);
        }
        return Result.success();
    }
}
