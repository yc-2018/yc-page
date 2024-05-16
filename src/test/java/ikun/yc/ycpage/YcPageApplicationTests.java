package ikun.yc.ycpage;

import ikun.yc.ycpage.entity.enumeration.MemoType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

//@SpringBootTest
class YcPageApplicationTests {

    @Test
    void contextLoads() {
//        System.out.println(MemoType.DIARY.toString());
//        System.out.println(MemoType.DIARY.getCode());
//        System.out.println(MemoType.DIARY.getName());
//        System.out.println(MemoType.DIARY.name());
//
//        MemoType[] values = MemoType.values();
//        for (MemoType value : values) {
//            System.out.println(value.getCode());
//            System.out.println(value.getName());
//            System.out.println(value.name());
//            System.out.println("-----");
//        }
        System.out.println(MemoType.valueOf("DIARY"));
    }

}
