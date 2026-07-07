package ikun.yc.ycpage.service;

import ikun.yc.ycpage.entity.LoopMemoItem;
import com.baomidou.mybatisplus.extension.service.IService;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferRequest;
import ikun.yc.ycpage.entity.dto.LoopMemoItemTransferResponse;

/**
 * <p>
 * 目前作为:循环代办的历史时间。 服务类
 * </p>
 *
 * @author chengguanglong
 * @since 2023-12-20
 */
public interface LoopMemoItemService extends IService<LoopMemoItem> {

    /**
     * 转移循环备忘记录到另一个循环备忘
     *
     * @param request 转移请求
     * @return 转移结果和两边最新循环次数
     */
    LoopMemoItemTransferResponse transferLoopMemoItems(LoopMemoItemTransferRequest request);
}
