package com.eventplanner.service;

import com.eventplanner.model.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.CommercialFixtures.*;
import static com.eventplanner.support.ExecutionFixtures.event;

class BudgetServiceTest {
    private final BudgetService service = new BudgetService();
    @Test void calculatesDecimalTotalsRemainingAndShortfallExactly() {
        var budget = budget(item("a", "3", "0.10", BudgetItem.PriceStatus.ESTIMATED), item("b", "2", "25.50", BudgetItem.PriceStatus.CONFIRMED));
        assertEquals(amount("51.30"), service.totalExpenses(budget));
        assertEquals(amount("448.70"), service.remainingBudget(event(), budget));
        assertEquals(0, service.shortfall(event(), budget).signum());
        assertEquals(amount("0.30"), budget.items().get(0).total());
    }
    @Test void flagsOverBudgetWithoutChangingLimitAndBlocksApproval() {
        var event = event();
        var budget = budget(item("a", "30", "20", BudgetItem.PriceStatus.ESTIMATED));
        assertEquals(amount("100.00"), service.shortfall(event, budget));
        assertTrue(service.validateBudget(event, budget).stream().anyMatch(w -> w.startsWith("Over budget")));
        assertThrows(IllegalArgumentException.class, () -> service.saveBudget(event, budget));
        assertEquals(500, event.getTotalBudget()); assertNull(event.getBudget());
    }
    @Test void unknownPriceIsNotZeroAndIncompletePlanCannotBeApproved() {
        var item = item("a", "1", null, BudgetItem.PriceStatus.UNKNOWN);
        assertNull(item.total());
        var budget = budget(item);
        assertTrue(service.validateBudget(event(), budget).stream().anyMatch(w -> w.contains("incomplete")));
        assertThrows(IllegalArgumentException.class, () -> service.saveBudget(event(), budget));
        assertThrows(IllegalArgumentException.class, () -> item("x", "1", "0", BudgetItem.PriceStatus.UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> item("x", "1", null, BudgetItem.PriceStatus.CONFIRMED));
        assertEquals(amount("0.00"), item("free", "1", "0", BudgetItem.PriceStatus.CONFIRMED).total());
    }
    @Test void rejectsNegativeZeroAndInvalidQuantitiesCostsAndDuplicateIds() {
        for (String quantity : List.of("-1", "0")) assertThrows(IllegalArgumentException.class,
                () -> item("a", quantity, "5", BudgetItem.PriceStatus.ESTIMATED));
        assertThrows(IllegalArgumentException.class, () -> item("a", "1", "-0.01", BudgetItem.PriceStatus.ESTIMATED));
        assertThrows(NumberFormatException.class, () -> amount("NaN"));
        var item = item("a", "1", "5", BudgetItem.PriceStatus.ESTIMATED);
        assertThrows(IllegalArgumentException.class, () -> budget(item, item));
    }
    @Test void editingIsImmutableAndApprovalStoresTheSameEvent() {
        var event = event();
        var original = budget(item("a", "30", "5", BudgetItem.PriceStatus.ESTIMATED));
        var replacement = item("a", "30", "6", BudgetItem.PriceStatus.CONFIRMED);
        var revised = service.updateAllocation(original, replacement);
        assertEquals(amount("150.00"), service.totalExpenses(original));
        assertEquals(amount("180.00"), service.totalExpenses(revised));
        service.saveBudget(event, revised); assertSame(revised, event.getBudget());
        assertThrows(IllegalArgumentException.class, () -> service.updateAllocation(original, item("missing", "1", "2", BudgetItem.PriceStatus.ESTIMATED)));
        assertThrows(UnsupportedOperationException.class, () -> revised.items().clear());
    }
    @Test void blocksCurrencyMismatch() {
        var event = event(); event.configureBudgetNeeds("USD", List.of(), "None");
        var budget = budget(item("a", "1", "5", BudgetItem.PriceStatus.ESTIMATED));
        assertTrue(service.validateBudget(event, budget).stream().anyMatch(w -> w.contains("Currency")));
        assertThrows(IllegalArgumentException.class, () -> service.saveBudget(event, budget));
    }
}
