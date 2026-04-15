package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.Log;
import ikun.yc.ycpage.common.aop.CountControlAspect;
import ikun.yc.ycpage.common.exception.ParamException;
import ikun.yc.ycpage.entity.MiniAccountMemo;
import ikun.yc.ycpage.service.MiniAccountMemoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 账号备忘控制器
 *
 * @author yc
 * @since 2026-04-15 09:19
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/mini/accountMemo")
public class MiniAccountMemoController {
    private final MiniAccountMemoService miniAccountMemoService;

    /**
     * 新增账号备忘
     */
    @Log
    @PostMapping
    @CountControl(operationType = CountControlAspect.ADD, frequency = 10)
    public R<MiniAccountMemo> addMiniAccountMemo(@RequestBody MiniAccountMemo miniAccountMemo) {
        validateMiniAccountMemo(miniAccountMemo, false);

        Date now = new Date();
        miniAccountMemo.setId(null);
        miniAccountMemo.setCreatedAt(now);
        miniAccountMemo.setUpdatedAt(now);
        miniAccountMemo.setIsDeleted(0);

        boolean saveOk = miniAccountMemoService.save(miniAccountMemo);
        return saveOk ? R.success(miniAccountMemo) : R.error("新增失败");
    }

    /**
     * 获取账号备忘列表
     */
    @GetMapping
    public R<Page<MiniAccountMemo>> getMiniAccountMemoList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        Page<MiniAccountMemo> memoPage = miniAccountMemoService.page(new Page<>(page, pageSize),
                Wrappers.<MiniAccountMemo>lambdaQuery()
                        .and(wrapper -> wrapper.eq(MiniAccountMemo::getIsDeleted, 0).or().isNull(MiniAccountMemo::getIsDeleted))
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(MiniAccountMemo::getWebsiteName, keyword)
                                .or().like(MiniAccountMemo::getWebsiteUrl, keyword)
                                .or().like(MiniAccountMemo::getAccount, keyword)
                                .or().like(MiniAccountMemo::getRemark, keyword))
                        .orderByDesc(MiniAccountMemo::getUpdatedAt)
                        .orderByDesc(MiniAccountMemo::getCreatedAt)
        );
        return R.success(memoPage);
    }

    /**
     * 获取单个账号备忘
     */
    @GetMapping("/{id}")
    public R<MiniAccountMemo> getMiniAccountMemo(@PathVariable Integer id) {
        if (id == null) throw new ParamException("id不能为空");

        MiniAccountMemo miniAccountMemo = miniAccountMemoService.getOne(Wrappers.<MiniAccountMemo>lambdaQuery()
                .eq(MiniAccountMemo::getId, id)
                .and(wrapper -> wrapper.eq(MiniAccountMemo::getIsDeleted, 0).or().isNull(MiniAccountMemo::getIsDeleted))
        );

        if (miniAccountMemo == null) return R.error("数据不存在");
        return R.success(miniAccountMemo);
    }

    /**
     * 修改账号备忘
     */
    @Log
    @PutMapping
    @CountControl(operationType = CountControlAspect.UPDATE)
    public R<MiniAccountMemo> updateMiniAccountMemo(@RequestBody MiniAccountMemo miniAccountMemo) {
        validateMiniAccountMemo(miniAccountMemo, true);

        boolean exists = miniAccountMemoService.count(Wrappers.<MiniAccountMemo>lambdaQuery()
                .eq(MiniAccountMemo::getId, miniAccountMemo.getId())
                .and(wrapper -> wrapper.eq(MiniAccountMemo::getIsDeleted, 0).or().isNull(MiniAccountMemo::getIsDeleted))
        ) > 0;
        if (!exists) return R.error("数据不存在");

        boolean updateOk = miniAccountMemoService.update(Wrappers.<MiniAccountMemo>lambdaUpdate()
                .eq(MiniAccountMemo::getId, miniAccountMemo.getId())
                .and(wrapper -> wrapper.eq(MiniAccountMemo::getIsDeleted, 0).or().isNull(MiniAccountMemo::getIsDeleted))
                .set(MiniAccountMemo::getWebsiteLogo, miniAccountMemo.getWebsiteLogo())
                .set(MiniAccountMemo::getWebsiteName, miniAccountMemo.getWebsiteName())
                .set(MiniAccountMemo::getWebsiteUrl, miniAccountMemo.getWebsiteUrl())
                .set(MiniAccountMemo::getAccount, miniAccountMemo.getAccount())
                .set(MiniAccountMemo::getPassword, miniAccountMemo.getPassword())
                .set(MiniAccountMemo::getRemark, miniAccountMemo.getRemark())
                .set(MiniAccountMemo::getUpdatedAt, new Date())
        );
        return updateOk ? R.success(miniAccountMemo) : R.error("修改失败");
    }

    /**
     * 删除账号备忘（软删除）
     */
    @Log
    @DeleteMapping("/{id}")
    @CountControl(operationType = CountControlAspect.DELETE)
    public R<Boolean> deleteMiniAccountMemo(@PathVariable Integer id) {
        if (id == null) throw new ParamException("id不能为空");

        boolean updateOk = miniAccountMemoService.update(Wrappers.<MiniAccountMemo>lambdaUpdate()
                .eq(MiniAccountMemo::getId, id)
                .and(wrapper -> wrapper.eq(MiniAccountMemo::getIsDeleted, 0).or().isNull(MiniAccountMemo::getIsDeleted))
                .set(MiniAccountMemo::getIsDeleted, 1)
                .set(MiniAccountMemo::getUpdatedAt, new Date())
        );
        return updateOk ? R.success(true) : R.error("删除失败或数据不存在");
    }

    private void validateMiniAccountMemo(MiniAccountMemo miniAccountMemo, boolean validateId) {
        if (miniAccountMemo == null) throw new ParamException("参数不能为空");
        if (validateId && miniAccountMemo.getId() == null) throw new ParamException("id不能为空");
        if (!StringUtils.hasText(miniAccountMemo.getWebsiteName())) throw new ParamException("网站名称不能为空");
//        if (!StringUtils.hasText(miniAccountMemo.getWebsiteUrl())) throw new ParamException("网站地址不能为空");
        if (!StringUtils.hasText(miniAccountMemo.getAccount())) throw new ParamException("账号不能为空");
//        if (!StringUtils.hasText(miniAccountMemo.getPassword())) throw new ParamException("密码不能为空");
    }
}
