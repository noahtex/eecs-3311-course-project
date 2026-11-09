package com.eventplanner;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.view.EventPlanningCLI;
import java.util.Scanner;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.FilterInputStream;

/** Separate entry point; does not initialize JavaFX. */
public final class CLIApp {
    private CLIApp() { }

    public static void main(String[] args) {
        EventService service = new EventService();
        EventPlanningFacade facade = new EventPlanningFacade(service);
        run(facade, System.in, System.out);
    }

    /** Close the owned scanner without closing Maven/IDE-owned console streams or terminals. */
    static void run(EventPlanningFacade facade, InputStream sharedInput, PrintStream output) {
        InputStream borrowed = new FilterInputStream(sharedInput) {
            @Override public void close() { /* The console host owns the underlying stream. */ }
        };
        try (Scanner input = new Scanner(borrowed)) {
            run(facade, input, output);
        }
    }

    /** Owns the facade; input/output are borrowed so embedded views do not close caller streams. */
    static void run(EventPlanningFacade facade, Scanner input, PrintStream output) {
        try (facade) {
            EventPlanningController controller = new EventPlanningController(facade);
            new EventPlanningCLI(controller, input, output).run();
        }
    }
}
