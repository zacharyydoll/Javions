package ch.epfl.javions;

/**
 * Represents a geographical position on Earth as a pair of longitude and latitude.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public record GeoPos(int longitudeT32, int latitudeT32) {

    /**
     * Represents a geographical position on Earth (i.e, a couple of longitude and latitude).
     *
     * @param longitudeT32 the longitude in T32 of a GeoPos instance
     * @param latitudeT32  the latitude in T32 of a GeoPos instance (must be between -90deg and +90deg)
     * @throws IllegalArgumentException if the inserted latitude is not between -90deg and +90deg
     */
    public GeoPos {
        Preconditions.checkArgument(isValidLatitudeT32(latitudeT32));
    }

    /**
     * Checks the validity of a given latitude in T32.
     *
     * @param latitudeT32 the latitude of the GeoPos instance
     * @return true if the value of the latitude is between -90deg and +90deg, false otherwise
     */
    public static boolean isValidLatitudeT32(int latitudeT32) {
        return ((latitudeT32 >= -(1 << 30)) && (latitudeT32 <= (1 << 30)));
    }

    /**
     * Returns the longitude of the GeoPos instance in radians
     *
     * @return the converted longitude of the GeoPos instance from T32 to radians
     */
    public double longitude() {
        return Units.convertFrom(longitudeT32, Units.Angle.T32);
    }

    /**
     * Returns the latitude of the GeoPos instance in radians
     *
     * @return the converted latitude of the GeoPos instance from T32 to radians
     */
    public double latitude() {
        return Units.convertFrom(latitudeT32, Units.Angle.T32);
    }

    /**
     * Returns the string of the GeoPos instance in degrees.
     *
     * @return the specified String format (longitude°, latitude°) in degrees
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(" + Units.convertTo(longitude(), Units.Angle.DEGREE) + "°, " +
                Units.convertTo(latitude(), Units.Angle.DEGREE) + "°)";
    }

}
