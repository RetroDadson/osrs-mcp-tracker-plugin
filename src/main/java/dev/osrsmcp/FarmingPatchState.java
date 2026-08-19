package dev.osrsmcp;

import com.google.gson.annotations.SerializedName;
import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Current farming patch states. The client only ever has ~5 "transmit" slots active at
 * once (VarbitID.FARMING_TRANSMIT_A..E), repurposed depending on which farming region the
 * player is near - RuneLite's own core plugin resolves these against a full region/patch
 * table to show real patch names and growth stages (e.g. "Catherby herb patch: harvestable").
 * <p>
 * This MVP deliberately does NOT attempt that full resolution. Beyond the missing patch
 * names, growth-stage thresholds are crop-type-specific (confirmed via RuneLite's own
 * PatchImplementation source - mushroom patches use different value ranges than allotments
 * or trees), and without the region/patch table we don't know which crop type occupies a
 * given slot. Guessing a "universal" threshold mapping risks silently wrong data. Instead,
 * only the one thing that IS safe to assert regardless of crop type is reported: whether a
 * slot is empty (raw value 0, a near-universal OSRS convention) or occupied (raw value
 * included for context/future decoding). Full state/name resolution is a deferred stretch
 * goal requiring the fuller table.
 */
public class FarmingPatchState implements ConsumableState {
    private final transient String playerName;
    private final List<Patch> patches;

    public static class Patch {
        public final String name;
        public final String state;
        @SerializedName("raw_value")
        public final int rawValue;

        public Patch(String name, String state, int rawValue) {
            this.name = name;
            this.state = state;
            this.rawValue = rawValue;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Patch)) return false;
            Patch other = (Patch) o;
            return name.equals(other.name) && rawValue == other.rawValue
                    && (state == null ? other.state == null : state.equals(other.state));
        }
    }

    public FarmingPatchState(String playerName, List<Patch> patches) {
        this.playerName = playerName;
        this.patches = patches;
    }

    /**
     * Pure, testable: raw value 0 is the one crop-type-independent signal we can safely
     * assert ("empty"). Any other value means something is planted, but which growth
     * stage depends on crop type - not decoded here, see class doc.
     */
    public static String stateForRawValue(int rawValue) {
        return rawValue == 0 ? "empty" : null;
    }

    public static FarmingPatchState fromClient(String playerName, Client client) {
        List<Patch> patches = new ArrayList<>();
        int[] slotVarbits = {
                VarbitID.FARMING_TRANSMIT_A,
                VarbitID.FARMING_TRANSMIT_B,
                VarbitID.FARMING_TRANSMIT_C,
                VarbitID.FARMING_TRANSMIT_D,
                VarbitID.FARMING_TRANSMIT_E,
        };
        char[] slotLetters = {'a', 'b', 'c', 'd', 'e'};

        for (int i = 0; i < slotVarbits.length; i++) {
            int value = client.getVarbitValue(slotVarbits[i]);
            patches.add(new Patch("patch_" + slotLetters[i], stateForRawValue(value), value));
        }

        return new FarmingPatchState(playerName, patches);
    }

    @Override
    public Object get() {
        return patches;
    }

    @Override
    public String whoOwnsThis() {
        return playerName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof FarmingPatchState)) return false;

        FarmingPatchState other = (FarmingPatchState) o;
        return patches.equals(other.patches);
    }
}
