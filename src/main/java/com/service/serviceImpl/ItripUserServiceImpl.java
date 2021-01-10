package com.service.serviceImpl;

import com.mapper.ItripUserMapper;
import com.po.ItripUser;
import com.service.ItripUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItripUserServiceImpl implements ItripUserService {
    @Autowired
    private ItripUserMapper itripUserMapper;
    @Override
    public boolean save(ItripUser itripUser) {//用户注册添加方法实现类
        int flag=itripUserMapper.insert(itripUser);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public ItripUser findItripUserByUserCode(String UserCode) {
        ItripUser itripUser=itripUserMapper.findItripUserByUserCode(UserCode);
        return itripUser;
    }

    @Override
    public int findactivated(String UserCode) {
        int flag=itripUserMapper.findactivated(UserCode);
        return flag;
    }

    @Override
    public int updateActivatedByUserCode(String UserCode) {
        int flag=itripUserMapper.updateActivatedByUserCode(UserCode);
        return flag;
    }

    @Override
    public ItripUser dologin(ItripUser itripUser) {
        return itripUserMapper.dologin(itripUser);
    }
}
