package ch.epfl.javions.adsb;

import ch.epfl.javions.GeoPos;

/**
 * Interface for setting the state of an aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public interface AircraftStateSetter {

    /**
     * Sets the timestamp of the last message received from the aircraft to the given value.
     *
     * @param timeStampNs the new timestamp value in nanoseconds
     */
    void setLastMessageTimeStampNs(long timeStampNs);

    /**
     * Sets the category of an aircraft to the given value.
     *
     * @param category the new category value
     */
    void setCategory(int category);

    /**
     * Sets the Call sign of an aircraft to the given value.
     *
     * @param callSign the new Call sign value
     */
    void setCallSign(CallSign callSign);

    /**
     * Sets the position of an aircraft to the given value.
     *
     * @param position the new position value as a GeoPos object
     */
    void setPosition(GeoPos position);

    /**
     * Sets the altitude of an aircraft to the given value.
     *
     * @param altitude the new altitude value in meters
     */
    void setAltitude(double altitude);

    /**
     * Sets the velocity of an aircraft to the given value.
     *
     * @param velocity the new velocity value in meters per second
     */
    void setVelocity(double velocity);

    /**
     * Sets the track or heading of the aircraft to the given value.
     *
     * @param trackOrHeading the new track or heading value in radians
     */
    void setTrackOrHeading(double trackOrHeading);
}
