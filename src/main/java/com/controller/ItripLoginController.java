package com.controller;

import com.alibaba.fastjson.JSONObject;
import com.po.Dto;
import com.po.ItripUser;
import com.service.ItripUserService;
import com.util.*;
import com.util.vo.ItripTokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;

@RestController
@RequestMapping(value = "/api")
public class ItripLoginController {    //用户登录.......
    @Autowired
    private ItripUserService itripUserService;
    Jedis jedis=new Jedis("127.0.0.1",6379);
    /********************用户登录************************************************/
    @RequestMapping(value = "/dologin")
    public Dto dologin(HttpServletRequest request, HttpServletResponse response,String name, String password){
        System.out.println("登录用户.....");
        if(!EmptyUtils.isEmpty(name)&&!EmptyUtils.isEmpty(password)){  //判断前台传来的登录信息不为空
            ItripUser itripUser=itripUserService.dologin(new ItripUser(name.trim(),MD5Util.getMd5(password.trim(),32)));
            //System.out.println(itripUser.toString());
            if(EmptyUtils.isNotEmpty(itripUser)){   //该用户存在
                if(jedis.get(name)==null){   //判断该用户是否处于登录状态，redis中没有token表示没有处于登录状态
                //登录成功，生成token令牌存到redis缓存和浏览器cookie中
                    String token=null;
                    token= TokenUtil.getTokenGenerator(request.getHeader("user-agent"),itripUser);
                if(token.startsWith("token:PC-")){   //通过获取到token字符串的头，判断是不是PC端登录
                  jedis.setex(name,3600,token);  //将token存储到redis中，用户名作为key值
                    //以token作为key值，将用户信息存储到redis中，方便后期其他业务操作时用到用户信息
                    String jsonItripUser= JSONObject.toJSONString(itripUser);
                    jedis.setex(token,3600,jsonItripUser);
                    //将token存储到浏览器cookie中，并设置其有效时长
                    ItripTokenVO itripTokenVO=new ItripTokenVO(token, Calendar.getInstance().getTimeInMillis()+60*60*1000,
                            Calendar.getInstance().getTimeInMillis());//token令牌，token失效时间，token开始时间
                    return DtoUtil.returnDataSuccess(itripTokenVO);
                }
                }else{   //redis中有token，处于登录状态
                    try {   //置换上一次登录的token
                        String newtoken=null;
                        newtoken= TokenUtil.replaceToken(request.getHeader("user-agent"),jedis.get(name));
                        if(newtoken!=null){
                            if(newtoken.startsWith("token:PC-")){
                                jedis.del(jedis.get(name));   //删除redis缓存中用户上一次登录token信息
                                jedis.setex(name,3600,newtoken);  //存入新的token信息
                                //以token作为key值，将用户信息存储到redis中，方便后期其他业务操作时用到用户信息
                                String jsonItripUser= JSONObject.toJSONString(itripUser);
                                jedis.setex(newtoken,3600,jsonItripUser);
                                //将token存储到浏览器cookie中，并设置其有效时长
                                ItripTokenVO itripTokenVO=new ItripTokenVO(newtoken, Calendar.getInstance().getTimeInMillis()+60*60*1000,
                                        Calendar.getInstance().getTimeInMillis());//token令牌，token失效时间，token开始时间
                                return DtoUtil.returnDataSuccess(itripTokenVO);
                            }
                        }
                    } catch (TokenValidationFailedException e) {
                        return DtoUtil.returnFail(e.getMessage(),"30008");
                    }

                }
            }else{   //（多种情况：1.该用户不存在、2.账号密码错误、3.账号未激活）

                return DtoUtil.returnFail("该用户不存在或账号密码错误....","30003");

            }

        }else{   //登录前台传来信息为空
            return DtoUtil.returnFail("登录信息不能为空....","30001");
        }
        return null;
    }

    /**************注销******************************************/
    /*
    * 注意：1.注销时，redis中token需要删除，前台浏览器token信息会自动覆盖
    *      2.注销时,判断是否处于登录状态（token令牌是否在有效期）
    * */
    @RequestMapping(value = "/logout")
    public Dto logout(HttpServletRequest request,HttpServletResponse response){
        System.out.println("注销....");
        //从浏览器拿到token
        String token=request.getHeader("token");
        //根据token获取到用户对象
        JSONObject jsonObject=JSONObject.parseObject(jedis.get(token));  //从rediS中获取到字符串对象
        ItripUser itripUser=JSONObject.toJavaObject(jsonObject,ItripUser.class);  //将字符串对象转换为java对象

        //验证token是否还有效（一小时的生命周期）
        if(TokenUtil.validate(request.getHeader("user-agent"),token)){   //返回true代表有效
            //清除redis（不会覆盖）、和cookie(下次登录会自动覆盖，不需要清除)中存储的token信息
            jedis.del(token);
            jedis.del(itripUser.getUsercode());   //删除redis中储存的用户信息（很重要）

            return DtoUtil.returnSuccess("系统推出成功.....");
        }else {  //false代表为处于登录状态
            return DtoUtil.returnFail("用户已退出....","36366");

        }

    }
}
