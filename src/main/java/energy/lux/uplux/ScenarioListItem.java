package energy.lux.uplux;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ScenarioListItem {
    String id;
    Instant createdAt;
    String name;
}
