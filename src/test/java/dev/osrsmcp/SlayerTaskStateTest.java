package dev.osrsmcp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SlayerTaskStateTest {

    @Test
    public void zeroRemainingIsCompleted() {
        assertEquals("completed", SlayerTaskState.inferOutcome(0));
    }

    @Test
    public void negativeRemainingIsCompleted() {
        assertEquals("completed", SlayerTaskState.inferOutcome(-1));
    }

    @Test
    public void positiveRemainingIsUnknown() {
        assertEquals("unknown", SlayerTaskState.inferOutcome(5));
    }
}
