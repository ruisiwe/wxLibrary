package com.ruoyi.library.mapper;
import com.ruoyi.library.domain.WlVipRefund; import java.util.Date; import java.util.List; import org.apache.ibatis.annotations.Param;
/** 会员退款数据访问。 */
public interface WlVipRefundMapper
{
    WlVipRefund selectByOrderId(@Param("orderId")Long orderId); WlVipRefund selectByMerchantRefundNoForUpdate(@Param("merchantRefundNo")String no);
    List<WlVipRefund> selectList(WlVipRefund query); int insertRefund(WlVipRefund refund);
    int markAccepted(@Param("id")Long id,@Param("wechatRefundId")String wechatRefundId);
    int markSuccess(@Param("id")Long id,@Param("wechatRefundId")String wechatRefundId,@Param("successTime")Date successTime,
        @Param("reclaimedPoints")Long reclaimedPoints,@Param("unrecoveredPoints")Long unrecoveredPoints,@Param("entitlementRevoked")String entitlementRevoked);
    int markFailed(@Param("id")Long id,@Param("reason")String reason);
}
