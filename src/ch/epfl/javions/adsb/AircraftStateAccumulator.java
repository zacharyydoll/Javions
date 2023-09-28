package ch.epfl.javions.adsb;

import ch.epfl.javions.GeoPos;

import java.util.Objects;

/**
 * Accumulates the status of a single aircraft over time.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public class AircraftStateAccumulator<T extends AircraftStateSetter> {
    private final static double TEN_SECONDS_IN_NS = 10e+9;
    // The last two messages received by the aircraft, stored at the index corresponding to their parity.
    private final AirbornePositionMessage[] lastMessages = new AirbornePositionMessage[2];
    private final T stateSetter;

    /**
     * Instantiates a new Aircraft state accumulator, used to determine the state of an aircraft over time.
     *
     * @param stateSetter the object used to set the state of the aircraft (cannot be null)
     * @throws NullPointerException if the state setter is null
     */
    public AircraftStateAccumulator(T stateSetter) {
        Objects.requireNonNull(stateSetter, "state accumulator cannot be null");
        this.stateSetter = stateSetter;
    }

    /**
     * Checks if two messages sent by an aircraft can be used to decode its position.
     *
     * @param current  the current message
     * @param previous the previous message
     * @return false if either message is null or if the time difference between the two messages is
     * greater than 10 seconds, true otherwise
     */
    private static boolean canBeDecoded(AirbornePositionMessage current, AirbornePositionMessage previous) {
        if (current == null || previous == null) {
            return false;
        } else {
            return (Math.abs(current.timeStampNs() - previous.timeStampNs())) <= TEN_SECONDS_IN_NS;
        }
    }

    /**
     * Gets the object used to set the state of the aircraft.
     *
     * @return the object used to set the state of the aircraft
     */
    public T stateSetter() {
        return stateSetter;
    }

    /**
     * Updates the state of the aircraft with the information contained in the given message, according to its type.
     *
     * @param message the message to update the state with (must be of a supported type code)
     * @throws IllegalStateException if the type code of the message is not supported
     * @see RawMessage#typeCode() for the type codes supported by this method
     */
    public void update(Message message) {

        //time stamp can be set regardless of the message type.
        stateSetter.setLastMessageTimeStampNs(message.timeStampNs());

        switch (message) {

            case AircraftIdentificationMessage aim -> {
                stateSetter.setCallSign(aim.callSign());
                stateSetter.setCategory(aim.category());
            }

            case AirbornePositionMessage apm -> {
                stateSetter.setAltitude(apm.altitude());
                lastMessages[apm.parity()] = apm; //Store current message in the array at the index of its parity

                if (canBeDecoded(lastMessages[0], lastMessages[1])) {
                    GeoPos pos = CprDecoder.decodePosition(lastMessages[0].x(),
                            lastMessages[0].y(),
                            lastMessages[1].x(),
                            lastMessages[1].y(),
                            apm.parity());
                    if (pos != null) stateSetter.setPosition(pos);
                }
            }
            case AirborneVelocityMessage avm -> {
                stateSetter.setVelocity(avm.speed());
                stateSetter.setTrackOrHeading(avm.trackOrHeading());
            }
            default -> {
                throw new IllegalStateException("Unexpected value: " + message); // Should never happen
            }
        }
    }
}
