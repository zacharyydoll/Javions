package ch.epfl.javions.adsb;

import ch.epfl.javions.aircraft.IcaoAddress;

/**
 * Represents a message sent by an aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public interface Message {

    /**
     * Represents the time at which the message was received.
     *
     * @return the timestamp of the message in nanoseconds
     */
    long timeStampNs();

    /**
     * Represents the ICAO address of the sender.
     *
     * @return the ICAO address of the sender.
     */
    IcaoAddress icaoAddress();

}
