package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniAccountMemo;
import ikun.yc.ycpage.service.MiniAccountMemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Date;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mini/accountMemo")
public class MiniAccountMemoController {
    private final MiniAccountMemoService miniAccountMemoService;

    /**
     * 添加账户备忘录
     *
     * @param miniAccountMemo 迷你账户备忘录
     * @author cgl
     * @since 2026/04/15 11:19:55
     */
    @UserId(fieldName = "userOpenid")
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<Boolean> addMiniAccountMemo(@RequestBody MiniAccountMemo miniAccountMemo) {
        validateForSave(miniAccountMemo);

        miniAccountMemo.setId(null);
        miniAccountMemo.setCreatedAt(null);
        miniAccountMemo.setUpdatedAt(null);

        return R.success(miniAccountMemoService.save(miniAccountMemo));
    }

    @PostMapping("/list/{page}")
    public R<Page<MiniAccountMemo>> getMiniAccountMemoList(
            @RequestBody(required = false) MiniAccountMemo query,
            @PathVariable Integer page) {
        MiniAccountMemo memoQuery = query == null ? new MiniAccountMemo() : query;

        Page<MiniAccountMemo> recordsPage = miniAccountMemoService.page(new Page<>(page, 10),
                Wrappers.<MiniAccountMemo>lambdaQuery()
                        .eq(MiniAccountMemo::getUserOpenid, BaseContext.getCurrentId())
                        .eq(MiniAccountMemo::getIsDeleted, 0)
                        .like(StringUtils.hasText(memoQuery.getWebsiteName()), MiniAccountMemo::getWebsiteName, memoQuery.getWebsiteName())
                        .like(StringUtils.hasText(memoQuery.getWebsiteUrl()), MiniAccountMemo::getWebsiteUrl, memoQuery.getWebsiteUrl())
                        .like(StringUtils.hasText(memoQuery.getAccount()), MiniAccountMemo::getAccount, memoQuery.getAccount())
                        .like(StringUtils.hasText(memoQuery.getEmail()), MiniAccountMemo::getEmail, memoQuery.getEmail())
                        .like(StringUtils.hasText(memoQuery.getRemark()), MiniAccountMemo::getRemark, memoQuery.getRemark())
                        .orderByDesc(MiniAccountMemo::getUpdatedAt)
                        .orderByDesc(MiniAccountMemo::getCreatedAt)
        );
        return R.success(recordsPage);
    }

    @PostMapping("/update")
    @CountControl(operationType = CountControlAspect.UPDATE)
    public R<Boolean> updateMiniAccountMemo(@RequestBody MiniAccountMemo miniAccountMemo) {
        if (miniAccountMemo == null || miniAccountMemo.getId() == null) {
            return R.error("数据有误");
        }
        validateForUpdate(miniAccountMemo);

        boolean updateOk = miniAccountMemoService.update(Wrappers.<MiniAccountMemo>lambdaUpdate()
                .eq(MiniAccountMemo::getId, miniAccountMemo.getId())
                .eq(MiniAccountMemo::getUserOpenid, BaseContext.getCurrentId())
                .eq(MiniAccountMemo::getIsDeleted, 0)
                .set(StringUtils.hasText(miniAccountMemo.getWebsiteLogo()), MiniAccountMemo::getWebsiteLogo, miniAccountMemo.getWebsiteLogo())
                .set(StringUtils.hasText(miniAccountMemo.getWebsiteName()), MiniAccountMemo::getWebsiteName, miniAccountMemo.getWebsiteName())
                .set(StringUtils.hasText(miniAccountMemo.getWebsiteUrl()), MiniAccountMemo::getWebsiteUrl, miniAccountMemo.getWebsiteUrl())
                .set(StringUtils.hasText(miniAccountMemo.getAccount()), MiniAccountMemo::getAccount, miniAccountMemo.getAccount())
                .set(StringUtils.hasText(miniAccountMemo.getEmail()), MiniAccountMemo::getEmail, miniAccountMemo.getEmail())
                .set(StringUtils.hasText(miniAccountMemo.getPassword()), MiniAccountMemo::getPassword, miniAccountMemo.getPassword())
                .set(MiniAccountMemo::getRemark, miniAccountMemo.getRemark())
                .set(MiniAccountMemo::getUpdatedAt, LocalDate.now())
        );
        return updateOk ? R.success(true) : R.error("修改失败");
    }

    @PostMapping("/delete/{id}")
    @CountControl(operationType = CountControlAspect.DELETE)
    public R<Boolean> deleteMiniAccountMemo(@PathVariable Integer id) {
        boolean updateOk = miniAccountMemoService.update(Wrappers.<MiniAccountMemo>lambdaUpdate()
                .eq(MiniAccountMemo::getId, id)
                .eq(MiniAccountMemo::getUserOpenid, BaseContext.getCurrentId())
                .eq(MiniAccountMemo::getIsDeleted, 0)
                .set(MiniAccountMemo::getIsDeleted, 1)
                .set(MiniAccountMemo::getUpdatedAt, LocalDate.now())
        );
        return updateOk ? R.success(true) : R.error("删除失败");
    }

    private void validateForSave(MiniAccountMemo miniAccountMemo) {
        if (miniAccountMemo == null) {
            throw new ParamException("参数不能为空");
        }
        if (!StringUtils.hasText(miniAccountMemo.getWebsiteName())) {
            throw new ParamException("网站名称不能为空");
        }
        if (!StringUtils.hasText(miniAccountMemo.getAccount())) {
            throw new ParamException("账号不能为空");
        }
    }

    private void validateForUpdate(MiniAccountMemo miniAccountMemo) {
        if (!StringUtils.hasText(miniAccountMemo.getWebsiteName())
                && !StringUtils.hasText(miniAccountMemo.getWebsiteLogo())
                && !StringUtils.hasText(miniAccountMemo.getWebsiteUrl())
                && !StringUtils.hasText(miniAccountMemo.getAccount())
                && !StringUtils.hasText(miniAccountMemo.getEmail())
                && !StringUtils.hasText(miniAccountMemo.getPassword())
                && miniAccountMemo.getRemark() == null) {
            throw new ParamException("没数");
        }
    }
}
