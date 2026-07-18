package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlPointRule;
import com.ruoyi.library.dto.AdRewardRequest;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.service.PointService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 小程序积分余额、任务奖励和流水接口。 */
@RestController
@RequestMapping("/wx/points")
public class WxPointController
{
    private final PointService pointService;

    public WxPointController(PointService pointService) { this.pointService = pointService; }

    /** 查询当前用户积分余额。 */
    @GetMapping("/balance")
    public WxApiResponse<Map<String, Long>> balance()
    {
        return WxApiResponse.success(Collections.singletonMap("pointBalance",
                pointService.getBalance(WxUserContext.get())));
    }

    /** 分页查询当前用户积分流水。 */
    @GetMapping("/records")
    public WxApiResponse<PageResult<WlPointRecord>> records(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize)
    {
        return WxApiResponse.success(pointService.listRecords(WxUserContext.get(), pageNum, pageSize));
    }

    /** 查询当前启用的积分任务规则。 */
    @GetMapping("/rules")
    public WxApiResponse<List<WlPointRule>> rules()
    {
        WlPointRule query = new WlPointRule();
        query.setStatus("0");
        return WxApiResponse.success(pointService.listRules(query));
    }

    /** 领取每日签到奖励，同一天重复请求直接返回已有流水。 */
    @PostMapping("/signin")
    public WxApiResponse<WlPointRecord> signin()
    {
        return WxApiResponse.success(pointService.signIn(WxUserContext.get()));
    }

    /** 完整观看激励视频后领取奖励，每日次数受规则限制。 */
    @PostMapping("/ad-reward")
    public WxApiResponse<WlPointRecord> adReward(@RequestBody AdRewardRequest request)
    {
        return WxApiResponse.success(pointService.rewardAd(WxUserContext.get(),
                request == null ? null : request.getAdBizNo()));
    }

    /** 领取每日首次分享奖励。 */
    @PostMapping("/share")
    public WxApiResponse<WlPointRecord> share()
    {
        return WxApiResponse.success(pointService.rewardShare(WxUserContext.get()));
    }
}
