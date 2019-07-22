package rep.mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class OriginalRecord {
    private String email;
    private String lastName; // only used in record 2194
    private Double debit;
    private Double creditEarned;
    private LocalDateTime cancelDateTime;
    private LocalDateTime enrollDateTime;
    private String experimentId;
    private String removeExpDelete;
    private String code;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate sessionDate;
}
