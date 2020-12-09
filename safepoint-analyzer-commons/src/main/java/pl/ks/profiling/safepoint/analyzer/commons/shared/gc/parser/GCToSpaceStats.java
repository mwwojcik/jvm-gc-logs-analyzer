package pl.ks.profiling.safepoint.analyzer.commons.shared.gc.parser;

import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PACKAGE)
public class GCToSpaceStats {
    private long sequenceId;
    private Map<String, String> regionStats = new HashMap<>();
}
