package com.repcheck.api.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weekly set landmarks per muscle group (hard sets per week).
 * MEV = minimum effective volume, MAV = top of the productive range,
 * MRV = maximum recoverable volume. Approximate, intermediate-lifter values.
 */
public final class VolumeLandmarks {

    public record Landmarks(int mev, int mav, int mrv) {
    }

    private static final Map<String, Landmarks> BY_MUSCLE = new LinkedHashMap<>();

    static {
        BY_MUSCLE.put("CHEST", new Landmarks(8, 16, 22));
        BY_MUSCLE.put("BACK", new Landmarks(10, 18, 25));
        BY_MUSCLE.put("SHOULDERS", new Landmarks(8, 16, 22));
        BY_MUSCLE.put("BICEPS", new Landmarks(6, 14, 20));
        BY_MUSCLE.put("TRICEPS", new Landmarks(6, 12, 18));
        BY_MUSCLE.put("QUADS", new Landmarks(8, 14, 20));
        BY_MUSCLE.put("HAMSTRINGS", new Landmarks(6, 12, 18));
        BY_MUSCLE.put("GLUTES", new Landmarks(4, 10, 16));
        BY_MUSCLE.put("CALVES", new Landmarks(8, 12, 20));
        BY_MUSCLE.put("ABS", new Landmarks(6, 16, 25));
    }

    private static final Landmarks DEFAULT = new Landmarks(6, 12, 20);

    private VolumeLandmarks() {
    }

    public static List<String> muscleGroups() {
        return List.copyOf(BY_MUSCLE.keySet());
    }

    public static boolean isKnown(String muscleGroup) {
        return BY_MUSCLE.containsKey(muscleGroup);
    }

    public static Landmarks forMuscle(String muscleGroup) {
        return BY_MUSCLE.getOrDefault(muscleGroup, DEFAULT);
    }
}
