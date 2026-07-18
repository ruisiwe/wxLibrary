package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/** 会员订单全额退款记录。 */
public class WlVipRefund extends BaseEntity
{
    private static final long serialVersionUID=1L;
    private Long id; private Long orderId; private Long userId; private String merchantRefundNo;
    private String wechatRefundId; private Long refundAmountCent; private String refundStatus;
    private Long shouldReclaimPoints; private Long reclaimedPoints; private Long unrecoveredPoints;
    private String entitlementRevoked; private Date successTime; private String failureReason;
    private Long operatorId; private String reason; private Date acceptedTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getMerchantRefundNo(){return merchantRefundNo;} public void setMerchantRefundNo(String v){merchantRefundNo=v;}
    public String getWechatRefundId(){return wechatRefundId;} public void setWechatRefundId(String v){wechatRefundId=v;}
    public Long getRefundAmountCent(){return refundAmountCent;} public void setRefundAmountCent(Long v){refundAmountCent=v;}
    public String getRefundStatus(){return refundStatus;} public void setRefundStatus(String v){refundStatus=v;}
    public Long getShouldReclaimPoints(){return shouldReclaimPoints;} public void setShouldReclaimPoints(Long v){shouldReclaimPoints=v;}
    public Long getReclaimedPoints(){return reclaimedPoints;} public void setReclaimedPoints(Long v){reclaimedPoints=v;}
    public Long getUnrecoveredPoints(){return unrecoveredPoints;} public void setUnrecoveredPoints(Long v){unrecoveredPoints=v;}
    public String getEntitlementRevoked(){return entitlementRevoked;} public void setEntitlementRevoked(String v){entitlementRevoked=v;}
    public Date getSuccessTime(){return successTime;} public void setSuccessTime(Date v){successTime=v;}
    public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
    public Long getOperatorId(){return operatorId;} public void setOperatorId(Long v){operatorId=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public Date getAcceptedTime(){return acceptedTime;} public void setAcceptedTime(Date v){acceptedTime=v;}
}
