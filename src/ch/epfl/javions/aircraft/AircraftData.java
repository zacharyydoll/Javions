package ch.epfl.javions.aircraft;

import java.util.Objects;

/**
 * Represents the gathering of data of an aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AircraftData(AircraftRegistration registration,
                           AircraftTypeDesignator typeDesignator,
                           String model,
                           AircraftDescription description,
                           WakeTurbulenceCategory wakeTurbulenceCategory) {
    /**
     * Creates a new AircraftData from the given parameters.
     *
     * @param registration           the Registration type of the Aircraft, cannot be null
     * @param typeDesignator         the Type Designator of the Aircraft, cannot be null
     * @param model                  the model of the Aircraft, cannot be null
     * @param description            the description of the Aircraft, cannot be null
     * @param wakeTurbulenceCategory the Turbulence category of the aircraft, cannot be null
     * @throws NullPointerException if any argument is null
     */
    public AircraftData {
        Objects.requireNonNull(registration, "Registration cannot be null");
        Objects.requireNonNull(typeDesignator, "Type Designator cannot be null");
        Objects.requireNonNull(model, "Model cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");
        Objects.requireNonNull(wakeTurbulenceCategory, "Wake Turbulence Category cannot be null");
    }
}
