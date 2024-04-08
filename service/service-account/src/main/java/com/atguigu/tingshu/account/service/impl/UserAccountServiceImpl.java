package com.atguigu.tingshu.account.service.impl;

import com.atguigu.tingshu.account.mapper.UserAccountDetailMapper;
import com.atguigu.tingshu.account.mapper.UserAccountMapper;
import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.model.account.UserAccount;
import com.atguigu.tingshu.model.account.UserAccountDetail;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserAccountServiceImpl extends ServiceImpl<UserAccountMapper, UserAccount> implements UserAccountService {

    @Autowired
    private UserAccountMapper userAccountMapper;

    @Autowired
    private UserAccountDetailMapper userAccountDetailMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initAccount(Long userId) {
        LambdaQueryWrapper<UserAccount> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccount::getUserId, userId);
        queryWrapper.select(UserAccount::getUserId);
        Long count = userAccountMapper.selectCount(queryWrapper);
        if (count > 0) {
            return;
        }
        BigDecimal giveMoney = new BigDecimal("100");
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userId);
        userAccount.setTotalAmount(giveMoney);
        userAccount.setLockAmount(new BigDecimal("0.00"));
        userAccount.setAvailableAmount(giveMoney);
        userAccount.setTotalIncomeAmount(new BigDecimal("0.00"));
        userAccount.setTotalPayAmount(new BigDecimal("0.00"));
        userAccountMapper.insert(userAccount);

        this.saveUserAccountDetail(userId, "充值赠送：", SystemConstant.ACCOUNT_TRADE_TYPE_DEPOSIT, giveMoney, null);
    }

    @Override
    public void saveUserAccountDetail(Long userId, String title, String tradeType, BigDecimal amount, String orderNo) {
        UserAccountDetail userAccountDetail = new UserAccountDetail();
        userAccountDetail.setUserId(userId);
        userAccountDetail.setTitle(title);
        userAccountDetail.setTradeType(tradeType);
        userAccountDetail.setAmount(amount);
        userAccountDetail.setOrderNo(orderNo);
        userAccountDetailMapper.insert(userAccountDetail);
    }
}
