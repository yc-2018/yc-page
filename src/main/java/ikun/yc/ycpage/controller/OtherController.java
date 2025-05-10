//仰晨study 创建时间2024/1/11 23:51 星期四
package ikun.yc.ycpage.controller;

import ikun.yc.ycpage.common.R;
import ikun.yc.ycpage.common.anno.CountControl;
import ikun.yc.ycpage.common.anno.PassToken;
import ikun.yc.ycpage.common.anno.UserId;
import ikun.yc.ycpage.entity.DySeeTime;
import ikun.yc.ycpage.entity.dto.DateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Paths;
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

}
