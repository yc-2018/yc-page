package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.entity.MiniCommonPhrase;
import ikun.yc.ycpage.service.MiniCommonPhraseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序打卡常用语控制器
 *
 * @author cgl
 * @since 2026/08/16
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/mini/commonPhrase")
public class MiniCommonPhraseController {
    private final MiniCommonPhraseService miniCommonPhraseService;

    /** 获取当前用户的全部常用语 */
    @GetMapping("/list")
    public R<List<MiniCommonPhrase>> list() {
        return R.success(miniCommonPhraseService.listCurrentUserPhrases());
    }

    /** 新增当前用户的常用语 */
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<Boolean> add(@RequestBody MiniCommonPhrase phrase) {
        return miniCommonPhraseService.addCurrentUserPhrase(phrase)
                ? R.success(true)
                : R.error("新增失败");
    }

    /** 修改当前用户的常用语 */
    @PostMapping("/update")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> update(@RequestBody MiniCommonPhrase phrase) {
        return miniCommonPhraseService.updateCurrentUserPhrase(phrase)
                ? R.success(true)
                : R.error("修改失败");
    }

    /** 将当前用户的指定常用语置顶 */
    @PostMapping("/top/{id}")
    @CountControl(operationType = CountControlAspect.UPDATE, frequency = 10)
    public R<Boolean> top(@PathVariable Integer id) {
        return miniCommonPhraseService.topCurrentUserPhrase(id)
                ? R.success(true)
                : R.error("置顶失败");
    }

    /** 删除当前用户的指定常用语 */
    @PostMapping("/delete/{id}")
    @CountControl(operationType = CountControlAspect.DELETE, frequency = 10)
    public R<Boolean> delete(@PathVariable Integer id) {
        return miniCommonPhraseService.deleteCurrentUserPhrase(id)
                ? R.success(true)
                : R.error("删除失败");
    }
}
