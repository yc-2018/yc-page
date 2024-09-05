package ikun.yc.ycpage.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
public class DateDto {
    private LocalDate date;
    private LocalDate startDate;
    private LocalDate endDate;

}
