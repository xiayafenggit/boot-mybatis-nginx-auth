package com.controller;


import com.po.Dto;
import com.po.ItripUser;
import com.service.ItripUserService;
import com.util.*;
import com.util.vo.ItripUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import java.util.Date;

@RestController
@RequestMapping(value ="/api")
public class ItripUserController {      //用户注册.....
    @Autowired
    private ItripUserService itripUserService;
    Jedis jedis=new Jedis("127.0.0.1",6379);
/********************** 注册账户信息 *****************************************/
    @RequestMapping(value ="/registerbyphone")
    public Dto registerbyphone(@RequestBody ItripUserVO itripUserVO){
        System.out.println("手机注册........");
        if(itripUserVO!=null&&!itripUserVO.equals("")){   //前台传来注册信息不为空
            //判断手机号是否已经注册过
         if(itripUserService.findItripUserByUserCode(itripUserVO.getUserCode())==null){  //手机号没有注册过
             ItripUser itripUser=new ItripUser();
             itripUser.setUsercode(itripUserVO.getUserCode());//存入用户登录昵称
             //对密码进行MD5方式加密
             String MD5_Passwd= MD5Util.getMd5(itripUserVO.getUserPassword(),32);
             itripUser.setUserpassword(MD5_Passwd);//存入用户登录密码
             itripUser.setUsertype(0);
             itripUser.setActivated(0);//存入状态信息（0表示未激活,1表示已经激活）
             itripUser.setUsername(itripUserVO.getUserName());//存入用户名
             //设置注册时间
             itripUser.setCreationdate(new Date());
             boolean flag=itripUserService.save(itripUser);
             if(flag){
                 //注册成功，发送激活码，并将激活码存到redis缓存中，有效时间2分钟
                 String sms=SMSUtil.testcheck(itripUser.getUsercode());//获取到激活码
                 jedis.setex(itripUserVO.getUserCode(),120,sms);
                 return DtoUtil.returnSuccess("注册成功，请激活，激活码已发送至手机（有效时长2min）...");
             }else {
                 return DtoUtil.returnFail("register fail...", ErrorCode.AUTH_AUTHENTICATION_UPDATE);
             }
         }else{  //手机号已经注册过
             //判断注册过的账号是否激活
             if (itripUserService.findactivated(itripUserVO.getUserCode())==0){  //未激活
                 //判断是否已经发送gou激活码(redis缓存中是否有激活码)
                 if(jedis.get(itripUserVO.getUserCode())==null){
                     String sms=SMSUtil.testcheck(itripUserVO.getUserCode());//获取到激活码
                     jedis.setex(itripUserVO.getUserCode(),120,sms);
                     return DtoUtil.returnFail("该账户已注册，未激活，激活码已发送至您手机（有效时长2min）...",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
                 }
             }else{   //以激活
              return DtoUtil.returnFail("该账号已存在，不能注册....",ErrorCode.AUTH_USER_ALREADY_EXISTS);
             }
         }
        }else{//注册信息为空
         return DtoUtil.returnFail("注册信息不能为空....",ErrorCode.AUTH_PARAMETER_ERROR);
        }
        return null;
    }
/*****************激活注册账户*******************************************/
    @RequestMapping(value = "/validatephone")
    public Dto validatephone(String user,String code){
        System.out.println("激活注册账户.....");
        //从缓存中获取激活码
        String oldsms=jedis.get(user);
        if(oldsms!=null){
            if(oldsms.equals(code)){  //比较缓存激活码与前台输入的激活码是否相等
                //激活码正确，修改数据库激活状态
               if(itripUserService.updateActivatedByUserCode(user)>1){
                  jedis.del(user);
                  return DtoUtil.returnSuccess("激活成功....");
               }
            }else{
                //激活码错误
            }
        }else{//缓存中没有激活码
        }
        return null;
    }
    /*****************激活手机或邮箱已有账号*******************************************/
    @RequestMapping(value = "/activate")
    public Dto activate(String user,String code){   //此方法为了弥补前台业务漏洞
        System.out.println("激活已有账号.....");

        if(itripUserService.findItripUserByUserCode(user)!=null){  //对前台传来的激活已有账号进行确认，防止输入错误
            String oldsms=jedis.get(user);
            //判断账号是邮箱还是手机号
            if(itripUserService.findItripUserByUserCode(user).getUsercode().indexOf("@")!=-1){  //是邮箱
                if (oldsms==null){   //没有激活码
                    String sms=SMSUtil.testcheck(user);//获取到激活码
                    jedis.setex(user,120,sms);
                    return DtoUtil.returnFail("激活码已发送至您邮箱.....",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
                }else{   //有激活码
                    if(oldsms.equals(code)){  //比较缓存激活码与前台输入的激活码是否相等
                        //激活码正确，修改数据库激活状态
                        if(itripUserService.updateActivatedByUserCode(user)>1){
                            jedis.del(user);
                            return DtoUtil.returnSuccess("激活成功....");
                        }
                    }else{
                        //激活码错误
                        String sms=SMSUtil.testcheck(user);//获取到激活码
                        jedis.setex(user,120,sms);
                        return DtoUtil.returnFail("激活码已发送至您邮箱.....",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
                    }
                }
            }else{   //是手机
                if (oldsms==null){   //没有激活码
                    String sms=SMSUtil.testcheck(user);//获取到激活码
                    jedis.setex(user,120,sms);
                    return DtoUtil.returnFail("激活码已发送至您手机.....",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
                }else{   //有激活码
                    if(oldsms.equals(code)){  //比较缓存激活码与前台输入的激活码是否相等
                        //激活码正确，修改数据库激活状态
                        if(itripUserService.updateActivatedByUserCode(user)>1){
                            jedis.del(user);
                            return DtoUtil.returnSuccess("激活成功....");
                        }
                    }else{
                        //激活码错误
                        String sms=SMSUtil.testcheck(user);//获取到激活码
                        jedis.setex(user,120,sms);
                        return DtoUtil.returnFail("激活码已发送至您手机.....",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
                    }
                }
            }

        }else{  //对已有账号进行激活时前台输入错误信息
            return DtoUtil.returnFail("未注册，请注册后再激活.....",ErrorCode.AUTH_EXIST_NOActivated_FAILED);
        }

       return null;
    }
    /******************邮箱******************************************/
    @RequestMapping(value = "/ckusr")
   public Dto ckusr(String name){  //验证邮箱是否已经注册过
       System.out.println("验证邮箱是否已经注册...");
     if(itripUserService.findItripUserByUserCode(name)==null){  //没有注册过
         return DtoUtil.returnSuccess("该邮箱可以注册...");
     }else {  //邮箱已经注册过
         return DtoUtil.returnFail("该邮箱已经被注册...","30009");
     }
   }
   /*********邮箱注册***************/
  @RequestMapping(value = "/doregister")
   public Dto doregister(@RequestBody ItripUserVO itripUserVO){
      System.out.println("邮箱注册...");

       if(itripUserVO!=null&&!itripUserVO.equals("")){
           if(itripUserService.findItripUserByUserCode(itripUserVO.getUserCode())==null){
               ItripUser itripUser=new ItripUser();
               itripUser.setUsercode(itripUserVO.getUserCode());
               itripUser.setUsername(itripUserVO.getUserName());
               String MD5_passwd=MD5Util.getMd5(itripUserVO.getUserPassword(),32);
               itripUser.setUserpassword(MD5_passwd);
               itripUser.setCreationdate(new Date());
               itripUser.setActivated(0);
               itripUser.setUsertype(0);
               if(itripUserService.save(itripUser)){
                   String emailsms=EmailUtil.emailregister(itripUser);//获取到邮件激活码
                   jedis.setex(itripUser.getUsercode(),120,emailsms);//将邮件激活码存入jedis缓存
                   return DtoUtil.returnSuccess("注册成功，激活码已发送至邮箱（有效时长2min）...");
               }else{
                   return DtoUtil.returnFail("注册失败...","32320");
               }
           }else{
               return DtoUtil.returnFail("注册失败,该账户已经注册...","32320");
           }

       }
       return null;
   }

}
