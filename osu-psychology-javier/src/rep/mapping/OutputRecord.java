package rep.mapping;

import java.time.LocalDate;

import lombok.Data;

@Data
public class OutputRecord {
    private Integer day; // ignore for now
    private LocalDate date;
    private String participantId; // use email for now
    private double creditsEarned;
    private double cumulativeCreditsEarned;
    private long enrolledCount;
    private long participatedCount;
    

    private long cancelCount;
    private boolean recievedTreatment;    

    private long missedCount;
    private double creditsMissed;
    private double cumulativeCreditsMissed;
}
