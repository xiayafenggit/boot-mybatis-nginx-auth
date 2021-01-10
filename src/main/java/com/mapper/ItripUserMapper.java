package com.mapper;

import com.po.ItripUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItripUserMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ItripUser record);//用户注册添加方法

    int insertSelective(ItripUser record);

    ItripUser selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ItripUser record);

    int updateByPrimaryKey(ItripUser record);
    public ItripUser findItripUserByUserCode(String UserCode);//根据手机号判断该号是否已经注册
    public int findactivated(String UserCode);//根据手机号判断该注册号码是否激活
    public int updateActivatedByUserCode(String UserCode);//激活账户
    public ItripUser dologin(ItripUser itripUser);//登录（传入对象（注册号，密码，状态））
}