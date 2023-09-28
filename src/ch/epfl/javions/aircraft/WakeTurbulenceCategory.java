package ch.epfl.javions.aircraft;

/**
 * Indicates the significance of the turbulences caused by an aircraft within an area.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public enum WakeTurbulenceCategory {
    LIGHT,
    MEDIUM,
    HEAVY,
    UNKNOWN;

    /**
     * Provides the wake turbulence category of the aircraft based on the inputted String.
     *
     * @param string the String of the WakeTurbulenceCategory of the Aircraft
     * @return the WakeTurbulenceCategory corresponding to the inputted String,
     * or UNKNOWN if the String is not recognized
     */
    public static WakeTurbulenceCategory of(String string) {
        WakeTurbulenceCategory category;
        switch (string) {
            case "L" -> {
                category = LIGHT;
                return category;
            }
            case "M" -> {
                category = MEDIUM;
                return category;
            }
            case "H" -> {
                category = HEAVY;
                return category;
            }
        }
        return UNKNOWN;
    }
}
