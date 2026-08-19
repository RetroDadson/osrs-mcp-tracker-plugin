package dev.osrsmcp;

/** POST body for {@code /clue-completed}, matching the server's ClueCompletion DTO. */
public class ClueCompletion {
    public final String name;
    public final String tier;

    public ClueCompletion(String name, String tier) {
        this.name = name;
        this.tier = tier;
    }
}
