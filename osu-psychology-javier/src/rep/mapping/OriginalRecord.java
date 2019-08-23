package rep.mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class OriginalRecord {
	private String id;
    private Double creditMissed; //absence
    private Double creditEarned; //credit
    private LocalDateTime cancelDateTime; //cancel
    private LocalDateTime enrollDateTime; //enroll
    private String experimentId; //experiment
    private String sessionCode; //code
    private LocalDate sessionDate;
    private LocalTime startTime; // sessionTime
}
