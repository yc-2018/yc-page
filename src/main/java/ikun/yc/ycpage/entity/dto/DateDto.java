package ikun.yc.ycpage.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class DateDto {
    private Integer seeRange;
    private Instant startDate;
    private Instant endDate;

}
