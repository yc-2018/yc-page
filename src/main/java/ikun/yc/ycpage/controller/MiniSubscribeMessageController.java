package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.dto.MiniSubscribeMessageRecordRequest;
import ikun.yc.ycpage.service.MiniSubscribeMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序订阅消息控制器
 *
 * @author cgl
 * @since 2026/11/05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/subscribeMessage")
public class MiniSubscribeMessageController {
    private final MiniSubscribeMessageService miniSubscribeMessageService;

    /**
     * 记录用户已同意接收的订阅消息模板
     *
     * @param request 授权记录请求
     * @return 是否记录成功
     */
    @PostMapping("/record")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> record(@RequestBody @Valid MiniSubscribeMessageRecordRequest request) {
        miniSubscribeMessageService.recordAcceptedTemplates(BaseContext.getCurrentId(), request.getTemplateIds());
        return R.success(true);
    }
}
