package com.util;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;

import java.util.Date;
import java.util.HashMap;

public class SMSAUtil {   //阿里云短信第三方工具类
    public static String Smsshow(String ucode){
        System.out.println(ucode);
        //连接阿里云
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", "LTAI4GEkxonMTeWBy6QwaXbP", "fpPTqQFpaTpURwg0pm24SHcposz6Pv");
        IAcsClient client = new DefaultAcsClient(profile);
        //构建请求
        CommonRequest request = new CommonRequest();
        //下面的信息不要改
        request.setSysMethod(MethodType.POST);
        request.setSysDomain("dysmsapi.aliyuncs.com");
        request.setSysVersion("2017-05-25");
        request.setSysAction("SendSms");
        //自定义的参数（手机号、签名和模版CODE）
        request.putQueryParameter("PhoneNumbers", ucode);
        request.putQueryParameter("SignName", "ABC商城");
        request.putQueryParameter("TemplateCode", "SMS_");

        //构建一个短信验证码
        HashMap<String, Object> map = new HashMap<>();
        //获取当前时间毫秒的后四位作为验证码
        Date date=new Date();
        long datetime=date.getTime();
        //保留后四位数字为验证码
        String val=""+datetime;
        String smsck=val.substring(val.length()-4,val.length());
        System.out.println("验证码-->"+smsck);
        map.put("code", smsck);
        request.putQueryParameter("TemplateParam", JSONObject.toJSONString(map));
        //尝试发送
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }


        return smsck;
    }
}
