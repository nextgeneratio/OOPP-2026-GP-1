package lk.ac.ruhuna.fot.ams.data.dao;

import java.util.Map;
import lk.ac.ruhuna.fot.ams.security.session.AuthenticatedSession;

public interface DashboardDao {
    Map<String, String> loadMetrics(AuthenticatedSession session);
}
