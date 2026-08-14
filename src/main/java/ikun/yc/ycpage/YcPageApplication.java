package ikun.yc.ycpage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("ikun.yc.ycpage.mapper")
public class YcPageApplication {

    public static void main(String[] args) {
        SpringApplication.run(YcPageApplication.class, args);
        String asciiArt = """
           ,  ,
           \\\\ \\\\
           ) \\\\ \\\\    _p_
           )^\\)\\)))  /  *\\
            \\_|| || / /^`-'
   __       -\\ \\\\--/ /
 <'  \\\\___/   ___. )'
      `====\\ )___/\\\\
           //     `"       启动成功
           \\\\    /  \\
           `"
        """;

        System.out.println(asciiArt);
    }

}
