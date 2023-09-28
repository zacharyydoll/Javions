package ch.epfl.javions.adsb;

import ch.epfl.javions.Bits;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.Units;
import ch.epfl.javions.aircraft.IcaoAddress;

import java.util.Objects;

/**
 * Represents a message sent by an aircraft to report its velocity and heading, characterized by the type code 19.
 * A velocity message contains the time at which it was sent, the ICAO address of the aircraft, the velocity of the
 * aircraft, and the heading of the aircraft.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record AirborneVelocityMessage(long timeStampNs,
                                      IcaoAddress icaoAddress,
                                      double speed,
                                      double trackOrHeading) implements Message {

    private static final int SUB_TYPE_START = 48;
    private static final int SUB_TYPE_SIZE = 3;
    private static final int SPEED_INFO_START = 21;
    private static final int SPEED_INFO_SIZE = 22;
    private static final int SPEED_NORM_SIZE = 10;

    //--------------------------------------------------------------------------------
    /**
     * The ground speed attributes
     */
    private static final int DNS_SIZE = 1;
    private static final int VNS_START = 0;
    private static final int VEW_START = VNS_START + SPEED_NORM_SIZE + DNS_SIZE;
    private static final int DEW_IDX = VEW_START + SPEED_NORM_SIZE;
    private static final int DNS_IDX = VNS_START + SPEED_NORM_SIZE;

    //--------------------------------------------------------------------------------
    /**
     * The air speed attributes
     */
    private static final int SH_IDX = 21;
    private static final int AS_START = 0;
    private static final int HEADING_START = 11;

    /**
     * Instantiates a new message sent by an aircraft to report its velocity and heading.
     *
     * @param timeStampNs    the time at which the message was sent, in nanoseconds (must be positive)
     * @param icaoAddress    the ICAO address of the aircraft (cannot be null)
     * @param speed          The speed of the aircraft in meters per second (must be positive)
     * @param trackOrHeading The direction of the aircraft in radians (must be positive)
     * @throws NullPointerException     if the ICAO address is null
     * @throws IllegalArgumentException if the timestamp or speed or direction are strictly negative
     */
    public AirborneVelocityMessage {
        Preconditions.checkArgument(timeStampNs >= 0 && trackOrHeading >= 0 && speed >= 0);
        Objects.requireNonNull(icaoAddress, "icaoAddress is null");
    }

    /**
     * Returns the velocity message of an aircraft if the provided raw message is an aircraft velocity message.
     * Raw messages are identified as aircraft velocity messages if their type code is 19, and valid subtypes are
     * 1 and 2 for ground speed, and 3 and 4 for air speed. Any other subtype is considered to be invalid.
     *
     * @param rawMessage the Raw ADS-B message sent by the aircraft
     * @return The Aircraft Velocity Message corresponding to the provided raw message, or null if the raw message's
     * subtype is invalid, or if the speed cannot be determined based on the provided information.
     */
    public static AirborneVelocityMessage of(RawMessage rawMessage) {

        int subType = Bits.extractUInt(rawMessage.payload(), SUB_TYPE_START, SUB_TYPE_SIZE);
        int speedInfo = Bits.extractUInt(rawMessage.payload(), SPEED_INFO_START, SPEED_INFO_SIZE);

        if (subType == 1 || subType == 2) return groundSpeed(speedInfo, subType, rawMessage);
        if (subType == 3 || subType == 4) return airSpeed(speedInfo, subType, rawMessage);

        else return null;
    }

    /**
     * Returns the velocity message of an aircraft reporting its ground speed.
     *
     * @param speedInfo  the speed information of the aircraft
     * @param subType    the subtype of the message (1 or 2 for ground speed)
     * @param rawMessage the Raw ADS-B message of the aircraft
     * @return the ground speed Aircraft Velocity Message of the aircraft, or null if the speed cannot be determined
     * based on the provided information
     */
    private static AirborneVelocityMessage groundSpeed(int speedInfo, int subType, RawMessage rawMessage) {

        int vew = Bits.extractUInt(speedInfo, VEW_START, SPEED_NORM_SIZE) - 1;
        int vns = Bits.extractUInt(speedInfo, VNS_START, SPEED_NORM_SIZE) - 1;

        if (vns + 1 == 0 || vew + 1 == 0) return null;

        vew *= Bits.testBit(speedInfo, DEW_IDX) ? -1 : 1; // if dew == 1, vew = -vew
        vns *= Bits.testBit(speedInfo, DNS_IDX) ? -1 : 1; // if dns == 1, vew = -vew

        double angle = Math.atan2(vew, vns); // vx = vew, vy = vns
        double groundSpeed = Math.hypot(vew, vns); // vx = vew, vy = vns

        if (angle < 0) angle += Units.Angle.TURN; // add 2 * PI if angle is negative.
        groundSpeed *= subType * subType; // multiply by 1 if subType is 1 or 4 if subType is 2 (supersonic).

        return new AirborneVelocityMessage(rawMessage.timeStampNs(),
                rawMessage.icaoAddress(),
                Units.convertFrom(groundSpeed, Units.Speed.KNOT),
                angle);
    }

    /**
     * Returns the velocity message of an aircraft reporting its air speed.
     *
     * @param speedInfo  the speed information of the aircraft
     * @param subType    the subtype of the message (3 or 4 for air speed)
     * @param rawMessage the Raw ADS-B message of the aircraft
     * @return the air speed Aircraft Velocity Message of the aircraft, or null if the speed cannot be determined
     * based on the provided information
     */
    private static AirborneVelocityMessage airSpeed(int speedInfo, int subType, RawMessage rawMessage) {

        double heading = Bits.extractUInt(speedInfo, HEADING_START, SPEED_NORM_SIZE);
        double airSpeed = Bits.extractUInt(speedInfo, AS_START, SPEED_NORM_SIZE) - 1;

        if (airSpeed + 1 == 0) return null;

        if (Bits.testBit(speedInfo, SH_IDX)) heading = Math.scalb(heading, -10);
        else return null; // speed cannot be determined.

        airSpeed *= subType == 3 ? 1 : 4; //multiply by 1 if subType is 3 or 4 if subType is 4 (supersonic).

        return new AirborneVelocityMessage(rawMessage.timeStampNs(),
                rawMessage.icaoAddress(),
                Units.convertFrom(airSpeed, Units.Speed.KNOT),
                Units.convertFrom(heading, Units.Angle.TURN));
    }
}