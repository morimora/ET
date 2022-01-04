package net.et.contracts;

import net.et.states.LoanState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void hasAmountFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        LoanState.class.getDeclaredField("value");
        // Is the message field of the correct type?
        assert(LoanState.class.getDeclaredField("value").getType().equals(double.class));
    }
}