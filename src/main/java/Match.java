import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Match {
    private LocalDateTime start;
    private String team1;
    private String team2;
    private String tournament;
    private String sport;
    private String url;
}
