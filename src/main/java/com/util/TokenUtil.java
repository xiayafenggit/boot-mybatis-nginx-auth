package com.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.po.ItripUser;
import cz.mallat.uasparser.UserAgentInfo;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TokenUtil {     //token处理工具类
    private static String tokenPrefix = "token:";//统一加入 token前缀标识
    private static Jedis jedis = new Jedis("127.0.0.1", 6379);
    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
        this.tokenPrefix = tokenPrefix;
    }
    /***
     * @param agent Http头中的user-agent信息
     * @param us 用户信息
     * @return Token格式<br/>
     * 	PC：“前缀PC-USERCODE-USERID-CREATIONDATE-RONDEM[6位]”
     *  <br/>
     *  Android：“前缀ANDROID-USERCODE-USERID-CREATIONDATE-RONDEM[6位]”
     *  生成token
     */
    public static String getTokenGenerator(String agent, ItripUser us) {
        try {
            UserAgentInfo userAgentInfo = UserAgentUtil.getUasParser().parse(agent);
            System.out.println(userAgentInfo.toString());
            StringBuilder sb = new StringBuilder();
            sb.append(tokenPrefix);//统一前缀
            if (userAgentInfo.getDeviceType().equals(UserAgentInfo.UNKNOWN)) {
                System.out.println("44444444");
                if (UserAgentUtil.CheckAgent(agent)) {
                    sb.append("MOBILE-");
                } else {
                    sb.append("PC-");
                }
            } else if (userAgentInfo.getDeviceType()
                    .equals("Personal computer")) {

                sb.append("PC-");
            } else {
                sb.append("MOBILE-");
            }
                sb.append(MD5Util.getMd5(us.getUsercode(), 32) + "-");//加密用户名称
                sb.append(us.getId() + "-");
                sb.append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                        + "-");
                sb.append(MD5Util.getMd5(agent, 6));// 识别客户端的简化实现——6位MD5码
            System.out.println("tokenUtil生成：=："+sb);
            return sb.toString();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

       return  null;
    }
    /*token时间验证*/
    public static boolean validate(String agent, String token) {

        if (jedis.get(token)==null) {// token不存在
            return false;
        }
        try {
            Date TokenGenTime;// token生成时间
            String agentMD5;
            String[] tokenDetails = token.split("-");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            TokenGenTime = formatter.parse(tokenDetails[3]);
            long passed = Calendar.getInstance().getTimeInMillis()
                    - TokenGenTime.getTime();
            if(passed>60*60*1000)
                return false;
            agentMD5 = tokenDetails[4];
            if(MD5Util.getMd5(agent, 6).equals(agentMD5))
                return true;
        } catch (ParseException e) {
            return false;
        }
        return false;
    }
   /*删除token*/
    public static void delete(String token) {
        if (jedis.get(token)!=null) {
            jedis.del(token);
        }
    }
    /*置换token*/
    public static String replaceToken(String agent, String token)
            throws TokenValidationFailedException {

        // 验证旧token是否有效
        if (jedis.get(token)==null) {// token不存在
            throw new TokenValidationFailedException("未知的token或 token已过期");// 终止置换
        }
        Date TokenGenTime;// token生成时间
        try {
            String[] tokenDetails = token.split("-");
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            TokenGenTime = formatter.parse(tokenDetails[3]);
        } catch (ParseException e) {
            throw new TokenValidationFailedException("token格式错误:" + token);
        }

        long passed = Calendar.getInstance().getTimeInMillis()
                - TokenGenTime.getTime();// token已产生时间
        if (passed < 60000) {// 置换保护期内（2min）
            throw new TokenValidationFailedException("token处于置换保护期内，剩余"
                    + (60000 - passed) / 1000
                    + "(s),禁止置换");
        }
        // 置换token
        String newToken = "";
        //通过token获取用户的json格式字符串
        JSONObject jsonobject=JSONObject.parseObject(jedis.get(token));
        //将json对象字符串转对象
        ItripUser user=  JSONObject.toJavaObject(jsonobject,ItripUser.class);
        long ttl = jedis.ttl(token);// token有效期（剩余秒数 ）
        if (ttl > 0 || ttl == -1) {// 兼容手机与PC端的token在有效期
            newToken = getTokenGenerator(agent, user);
            //this.save(newToken, user);// 缓存新token
            //将token存入redis(因为将来取值时需要返回对象所以转成json存进去)
            String strJson = JSON.toJSONString(user);
            jedis.setex(newToken,3600,strJson);
            //jedis.setex(token, 7200,JSON.toJSONString(user));// 2分钟后旧token过期，注意手机端由永久有效变为2分钟（REPLACEMENT_DELAY默认值）后失效
        } else {// 其它未考虑情况，不予置换
            throw new TokenValidationFailedException("当前token的过期时间异常,禁止置换");
        }
        return newToken;
    }
}
