package dev.osrsmcp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FarmingPatchStateTest {

    @Test
    public void zeroValueIsEmpty() {
        assertEquals("empty", FarmingPatchState.stateForRawValue(0));
    }

    @Test
    public void nonZeroValueIsUnresolved() {
        assertNull(FarmingPatchState.stateForRawValue(1));
        assertNull(FarmingPatchState.stateForRawValue(12));
        assertNull(FarmingPatchState.stateForRawValue(25));
    }
}
