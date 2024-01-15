//仰晨study 创建时间2024/1/11 23:51 星期四
package ikun.yc.ycpage.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 其他控制器 不处理数据库相关
 *
 * @author ChenGuangLong
 * @since 2024/01/11 23:51:55
 */
@Slf4j
@RestController
@RequestMapping("/other")
public class OtherController {
    @GetMapping("/run-script")
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

}
