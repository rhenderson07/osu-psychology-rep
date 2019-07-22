package rep.mapping.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Participant {
    private String id; // email for now
    private List<Session> sessions;
}
