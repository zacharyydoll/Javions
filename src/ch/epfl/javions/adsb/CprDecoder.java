package ch.epfl.javions.adsb;

import ch.epfl.javions.GeoPos;
import ch.epfl.javions.Preconditions;
import ch.epfl.javions.Units;

/**
 * Decodes the CPR encoded position of an aircraft given its last two messages, and the coordinates they were
 * sent from.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class CprDecoder {
    private final static double NB_EVEN_LAT_ZONES = 60.0;
    private final static double NB_ODD_LAT_ZONES = 59.0;
    private final static double DELTA_0 = 1.0 / NB_EVEN_LAT_ZONES;
    private final static double DELTA_1 = 1.0 / NB_ODD_LAT_ZONES;
    private final static int EVEN_PARITY = 0;
    private final static int ODD_PARITY = 1;

    private CprDecoder() {
    } // Prevent instantiation

    /**
     * Gives the position of an aircraft given its last two messages, and the coordinates they were sent from.
     *
     * @param x0         the normalized local longitude of an even message. (between 0 and 1)
     * @param y0         the normalized local latitude of an even message. (between 0 and 1)
     * @param x1         the normalized local longitude of an odd message. (between 0 and 1)
     * @param y1         the normalized local latitude of an odd message. (between 0 and 1)
     * @param mostRecent the parity of the most recent message (must be 0 or 1)
     * @return the position of the aircraft, or null if the given latitudes are invalid or if the position
     * cannot be determined because the aircraft is switching latitudes.
     * @throws IllegalArgumentException if the given parity of the most recent message is not 0 or 1.
     */
    public static GeoPos decodePosition(double x0, double y0, double x1, double y1, int mostRecent) {

        Preconditions.checkArgument(mostRecent == EVEN_PARITY || mostRecent == ODD_PARITY);

        double[] latitudes = getAnglesInTurns(y0, y1, NB_ODD_LAT_ZONES, NB_EVEN_LAT_ZONES, DELTA_0, DELTA_1);
        if (checkValidLatitudes(latitudes[EVEN_PARITY], latitudes[ODD_PARITY])) return null;

        double[] longitudes = getLongitudes(latitudes[EVEN_PARITY], latitudes[ODD_PARITY], x0, x1);
        //longitudes cannot not be determined because the aircraft is switching latitudes.
        if (longitudes == null) return null;

        //returns the position using even angles if most recent parity is even, or odd angles if the parity is odd.
        return new GeoPos((int) Math.rint(convertTurnToT32(longitudes[mostRecent])),
                (int) Math.rint(convertTurnToT32(latitudes[mostRecent])));
    }

    /**
     * Gives the angles (latitudes or longitudes) in turns of both parities at time of sending.
     *
     * @param coord0 the normalized local coordinates of an even message. (y0 for latitude, x0 for longitude)
     * @param coord1 the normalized local coordinates of an odd message. (y1 for latitude, x1 for longitude)
     * @param z1     the number of (latitude or longitude) zones in the odd splitting.
     * @param z0     the number of (latitude or longitude) zones in the even splitting.
     * @param width0 the width of a zone in the even splitting in turns.
     * @param width1 the width of a zone in the odd splitting in turns.
     * @return a double array of size two, containing the angles in turns stored at their respective parities.
     */
    private static double[] getAnglesInTurns(double coord0,
                                             double coord1,
                                             double z1,
                                             double z0,
                                             double width0,
                                             double width1) {

        double zones = Math.rint((coord0 * z1) - (coord1 * z0));
        var nbOfEvenZones = zones;
        var nbOfOddZones = zones;

        if (zones < 0) {
            nbOfEvenZones = zones + z0;
            nbOfOddZones = zones + z1;
        }
        var angle0TURN = width0 * (nbOfEvenZones + coord0);
        var angle1TURN = width1 * (nbOfOddZones + coord1);

        //Recentering the angle that are greater than 0.5 by subtracting 1.
        return new double[]{angle0TURN >= 0.5 ? angle0TURN - 1 : angle0TURN,
                angle1TURN >= 0.5 ? angle1TURN - 1 : angle1TURN};
    }

    /**
     * Gives the longitudes in turns of both parities at time of sending.
     *
     * @param lat0 the latitude of the even parity message in turns.
     * @param lat1 the latitude of the odd parity message in turns.
     * @param x0   The normalized local longitude of an even message. (between 0 and 1)
     * @param x1   The normalized local longitude of an odd message. (between 0 and 1)
     * @return a double array of size two containing the two longitudes in turns, stored at their respective parities.
     */
    private static double[] getLongitudes(double lat0, double lat1, double x0, double x1) {
        double A = calculateA(lat0);
        double B = calculateA(lat1);

        var nbOfEvenLongZones = getLongitudeZones(A);
        var nbOfLongCheck = getLongitudeZones(B); //using B to check that we are not switching latitude bands.
        var nbOfOddLongZones = nbOfEvenLongZones - 1;

        //we define the number of zones of both longitude zones to be 1 when the aircraft nears poles.
        //TODO : change nbOfOddLongZones to 1 or B (was 1)
        if (Double.isNaN(A)) {
            nbOfEvenLongZones = 1;
            nbOfOddLongZones = B;
        }
        //return null if we are switching latitude bands.
        else if (nbOfEvenLongZones != nbOfLongCheck) return null;

        var evenLongZoneWidth = 1.0 / nbOfEvenLongZones;
        var oddLongZoneWidth = 1.0 / nbOfOddLongZones;
        return getAnglesInTurns(x0, x1, nbOfOddLongZones, nbOfEvenLongZones, evenLongZoneWidth, oddLongZoneWidth);
    }

    private static double calculateA(double latitude) {
        var num = 1 - Math.cos(Units.Angle.TURN * DELTA_0);
        var denom = Math.cos(Units.convert(latitude, Units.Angle.TURN, Units.Angle.RADIAN));
        double preOutput = num / (denom * denom);

        return Math.acos(1 - preOutput);
    }

    /**
     * Calculates the quotient of 2PI divided by the given double, and rounds down to the nearest integer.
     *
     * @param A the double to divide 2PI by.
     * @return the input divided by 2PI, rounded down to the nearest integer.
     */
    private static double getLongitudeZones(double A) {
        return Math.floor((Units.Angle.TURN / A));
    }

    /**
     * Checks the validity of the given latitudes.
     *
     * @param lat0Turn the latitude of the even parity message in turns.
     * @param lat1Turn the latitude of the odd parity message in turns.
     * @return true if the latitudes are not between -90 and 90 degrees, false otherwise.
     */
    private static boolean checkValidLatitudes(double lat0Turn, double lat1Turn) {
        return !(GeoPos.isValidLatitudeT32((int) convertTurnToT32(lat0Turn))
                && GeoPos.isValidLatitudeT32((int) convertTurnToT32(lat1Turn)));
    }

    /**
     * Converts an angle from turn to T32. Used to avoid long code duplications throughout the class.
     *
     * @param angleTURN angle in turn that we wish to convert to T32.
     * @return the angle converted form T32 to turn.
     */
    private static double convertTurnToT32(double angleTURN) {
        return Units.convert(angleTURN, Units.Angle.TURN, Units.Angle.T32);
    }


}



