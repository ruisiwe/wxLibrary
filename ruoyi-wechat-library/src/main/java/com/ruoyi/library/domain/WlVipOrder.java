package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/** 会员支付订单及不可变套餐快照。 */
public class WlVipOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id; private Long userId; private Long planId; private String merchantOrderNo;
    private String wechatTransactionId; private String planCodeSnapshot; private String planNameSnapshot;
    private Long amountCent; private String currency; private Integer validDaysSnapshot;
    private Long giftPointsSnapshot; private String orderStatus; private Date paidTime; private Date closedTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public Long getPlanId(){return planId;} public void setPlanId(Long v){planId=v;}
    public String getMerchantOrderNo(){return merchantOrderNo;} public void setMerchantOrderNo(String v){merchantOrderNo=v;}
    public String getWechatTransactionId(){return wechatTransactionId;} public void setWechatTransactionId(String v){wechatTransactionId=v;}
    public String getPlanCodeSnapshot(){return planCodeSnapshot;} public void setPlanCodeSnapshot(String v){planCodeSnapshot=v;}
    public String getPlanNameSnapshot(){return planNameSnapshot;} public void setPlanNameSnapshot(String v){planNameSnapshot=v;}
    public Long getAmountCent(){return amountCent;} public void setAmountCent(Long v){amountCent=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public Integer getValidDaysSnapshot(){return validDaysSnapshot;} public void setValidDaysSnapshot(Integer v){validDaysSnapshot=v;}
    public Long getGiftPointsSnapshot(){return giftPointsSnapshot;} public void setGiftPointsSnapshot(Long v){giftPointsSnapshot=v;}
    public String getOrderStatus(){return orderStatus;} public void setOrderStatus(String v){orderStatus=v;}
    public Date getPaidTime(){return paidTime;} public void setPaidTime(Date v){paidTime=v;}
    public Date getClosedTime(){return closedTime;} public void setClosedTime(Date v){closedTime=v;}
    public WlVipPlan toPlanSnapshot()
    {
        WlVipPlan plan=new WlVipPlan(); plan.setId(planId); plan.setPlanCode(planCodeSnapshot);
        plan.setPlanName(planNameSnapshot); plan.setPriceCent(amountCent); plan.setValidDays(validDaysSnapshot);
        plan.setGiftPoints(giftPointsSnapshot); plan.setStatus("0"); return plan;
    }
}
