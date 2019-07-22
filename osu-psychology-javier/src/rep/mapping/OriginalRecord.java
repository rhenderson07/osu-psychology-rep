package rep.mapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Data;

@Data
public class OriginalRecord {
	private Integer id;
    private String email;
    private Double debit; //absence
    private Double creditEarned; //credit
    private LocalDateTime cancelDateTime; //dropped
    private LocalDateTime enrollDateTime; //enroll
    private String experimentId; //experiment
    //private String removeExpDelete; 
    private String code; //sessionCode
    private LocalDate sessionDate;
    private LocalTime startTime; // sessionTime
    //private LocalTime endTime;
    private Integer eventDateOffset; //event_Date
}
