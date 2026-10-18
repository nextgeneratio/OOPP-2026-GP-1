package lk.ac.ruhuna.fot.ams.api.response;

import java.util.Map;

/** Display-ready dashboard figures keyed by card label. Computed by the business layer, only rendered by Swing. */
public record DashboardSummary(Map<String, String> metrics) {
    public DashboardSummary {
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    }

    public String value(String label) {
        return metrics.getOrDefault(label, "—");
    }
}
