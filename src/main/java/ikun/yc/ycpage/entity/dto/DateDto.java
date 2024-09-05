package ikun.yc.ycpage.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DateDto {
    private LocalDateTime date;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

}
