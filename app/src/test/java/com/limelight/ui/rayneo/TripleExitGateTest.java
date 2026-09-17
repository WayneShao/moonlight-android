package com.limelight.ui.rayneo;
import org.junit.Test;
import static org.junit.Assert.*;

public class TripleExitGateTest {
    @Test public void firstTripleOnlyArmsSecondConfirms() {
        TripleExitGate gate=new TripleExitGate();
        assertFalse(gate.confirm(100));assertTrue(gate.confirm(1000));assertFalse(gate.confirm(1500));
    }
    @Test public void expiredTripleArmsAgain() {
        TripleExitGate gate=new TripleExitGate();
        assertFalse(gate.confirm(100));assertFalse(gate.confirm(3200));assertTrue(gate.confirm(4000));
    }
    @Test public void focusLossCancelsConfirmation() {
        TripleExitGate gate=new TripleExitGate();
        assertFalse(gate.confirm(100));gate.reset();assertFalse(gate.confirm(500));
    }
}
