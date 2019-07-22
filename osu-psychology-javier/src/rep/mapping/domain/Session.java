package rep.mapping.domain;

import java.time.LocalDate;

import lombok.Data;

@Data
public class Session {
    private LocalDate sessionDate;
    private Double creditsEarned;
    private boolean enrolled;
    private boolean cancelled;
    private boolean attended;
}
