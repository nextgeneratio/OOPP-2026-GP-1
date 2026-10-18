package lk.ac.ruhuna.fot.ams.presentation.ui.common;

import lk.ac.ruhuna.fot.ams.api.controller.DashboardController;
import lk.ac.ruhuna.fot.ams.api.controller.UserController;

/** The API controllers the screens may call. Swing never receives services, DAOs or connections. */
public record UiServices(DashboardController dashboard, UserController users) {
}
