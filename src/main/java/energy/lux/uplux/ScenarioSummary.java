package energy.lux.uplux;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Metadata about a user scenario.
 * Not a Java record because AnyLogic doesn't have intellisense for records.
 */
@Value
@Builder
public class ScenarioSummary {
    String id;
    Instant createdAt;
    String name;
}
