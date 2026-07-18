package com.ruoyi.library.mapper;
import com.ruoyi.library.domain.WlVipOrder; import java.util.Date; import java.util.List; import org.apache.ibatis.annotations.Param;
/** 会员支付订单数据访问。 */
public interface WlVipOrderMapper
{
    WlVipOrder selectById(@Param("id") Long id); WlVipOrder selectByIdForUpdate(@Param("id") Long id);
    WlVipOrder selectByMerchantOrderNo(@Param("merchantOrderNo") String no);
    WlVipOrder selectByMerchantOrderNoForUpdate(@Param("merchantOrderNo") String no); List<WlVipOrder> selectList(WlVipOrder query);
    int insertOrder(WlVipOrder order); int markPrepayReady(@Param("id")Long id);
    int markPaid(@Param("id")Long id,@Param("transactionId")String transactionId,@Param("paidTime")Date paidTime);
    int markRefundProcessing(@Param("id")Long id); int markRefunded(@Param("id")Long id);
}
