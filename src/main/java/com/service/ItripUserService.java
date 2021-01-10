package com.service;

import com.po.ItripUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface ItripUserService {
    public boolean save(ItripUser itripUser);//用户注册添加方法
    public ItripUser findItripUserByUserCode(String UserCode);//根据手机号判断改号是否已经注册
    public int findactivated(String UserCode);//根据手机号判断该注册号码是否激活
    public int updateActivatedByUserCode(String UserCode);//激活账户
    public ItripUser dologin(ItripUser itripUser);//登录（传入对象（注册号，密码，状态））
}
