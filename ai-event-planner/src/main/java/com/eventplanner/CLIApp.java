package com.eventplanner;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.view.EventPlanningCLI;
import java.util.Scanner;

/** Separate entry point; does not initialize JavaFX. */
public final class CLIApp {
    private CLIApp() { }

    public static void main(String[] args) {
        EventService service = new EventService();
        EventPlanningFacade facade = new EventPlanningFacade(service);
        EventPlanningController controller = new EventPlanningController(facade);
        new EventPlanningCLI(controller, new Scanner(System.in), System.out).run();
    }
}
