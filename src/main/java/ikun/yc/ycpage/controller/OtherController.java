//仰晨study 创建时间2024/1/11 23:51 星期四
package ikun.yc.ycpage.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ikun.yc.ycpage.common.BaseContext;
import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.PassToken;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.entity.DeviceUsageLog;
import ikun.yc.ycpage.entity.DySeeTime;
import ikun.yc.ycpage.entity.dto.DateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 其他控制器
 *
 * @author ChenGuangLong
 * @since 2024/01/11 23:51:55
 */
@Slf4j
@RestController
@RequestMapping("/other")
public class OtherController {

    /** 开始运行时间 */
    private static final String START_RUNNING_TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @PassToken
    @GetMapping("startTime")
    public String startTime() {
        return START_RUNNING_TIME;
    }

    @PassToken
    @GetMapping("/run-script")
    @CountControl(frequency = 1)
    public String runScript(String scriptName) {
        try {
            // 判断操作系统是否为Linux
            String osName = System.getProperty("os.name").toLowerCase();
            if (!osName.contains("linux"))
                return "该脚本只能在Linux上运行。";

            // 指定要运行的脚本路径
            String scriptPath = "/var/script/"+scriptName;

            // 检查脚本是否存在
            if (!Files.exists(Paths.get(scriptPath)))
                return "脚本文件不存在。";

            // 在新的线程中执行脚本
            new Thread(() -> {
                try {
                    Process process = Runtime.getRuntime().exec(scriptPath);
                    process.waitFor();
                } catch (Exception e) {log.error(e.getMessage());}
            }).start();
            return "脚本在后台运行。";

        } catch (Exception e) {
            log.error(e.getMessage());
            return "执行脚本时出错。";
        }
    }

    /**
     * 记录dy看的时间
     *
     * @param dySeeTime 看的时间对象
     * @author ChenGuangLong
     * @since 2024/05/31 17:46:46
     */
    @UserId
    @PostMapping("/dySeeTime")
    public R<?> saveDySeeTime(@RequestBody DySeeTime dySeeTime) {
        return R.success(dySeeTime.checkLegal().updateOrInsert());
    }

    @PostMapping("/getSeeTime")
    public R<List<DySeeTime>> getSeeTime(@RequestBody DateDto dateDto) {
        if (dateDto == null || dateDto.getStartDate() == null || dateDto.getEndDate() == null)
            throw new IllegalArgumentException("<X_X>");
        if (dateDto.getSeeRange() == null || dateDto.getSeeRange() <= 0 || dateDto.getSeeRange() > 4)
            throw new IllegalArgumentException("《X_X》");
        return R.success(new DySeeTime().getSeeTimeByDate(dateDto));
    }

    /**
     * 记录设备使用日志
     *
     * @param deviceUsageLog 设备使用日志对象
     * @author cgl
     * @since 2025/11/12 01:43:12
     */
    @UserId
    @PostMapping("/deviceUsageLog")
    public R<?> saveDeviceUsageLog(@RequestBody DeviceUsageLog deviceUsageLog) {
        return R.success(deviceUsageLog.insert());
    }

    /**
     * 分页查询设备使用日志
     *
     * @param page      当前页码
     * @param pageSize  每页条数
     * @param name      App 名称关键字
     * @param startTime 创建时间起始时间戳
     * @param endTime   创建时间结束时间戳
     * @return 设备使用日志分页数据
     * @author cgl
     * @since 2026/05/12 15:40:00
     */
    @GetMapping("/deviceUsageLog")
    public R<Page<DeviceUsageLog>> getDeviceUsageLogList(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String name,
                                                         @RequestParam(required = false) Long startTime,
                                                         @RequestParam(required = false) Long endTime) {
        String appName = name == null ? null : name.trim(); // App 名称查询关键字
        boolean hasAppName = appName != null && !appName.isEmpty(); // 是否按 App 名称过滤
        Instant startInstant = startTime == null ? null : Instant.ofEpochMilli(startTime); // 创建时间起始值
        Instant endInstant = endTime == null ? null : Instant.ofEpochMilli(endTime); // 创建时间结束值

        return R.success(new DeviceUsageLog().selectPage(new Page<>(page, pageSize),
                Wrappers.<DeviceUsageLog>lambdaQuery()
                        .eq(DeviceUsageLog::getUserId, BaseContext.getCurrentId())
                        .like(hasAppName, DeviceUsageLog::getName, appName)
                        .ge(startInstant != null, DeviceUsageLog::getTime, startInstant)
                        .le(endInstant != null, DeviceUsageLog::getTime, endInstant)
                        .orderByDesc(DeviceUsageLog::getTime)
        ));
    }

}
