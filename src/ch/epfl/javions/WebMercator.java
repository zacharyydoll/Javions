package ch.epfl.javions;

/**
 * Class containing methods used to convert longitudes and latitudes to x and y coordinates on
 * a Mercator projection.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class WebMercator {
    private WebMercator() {
    } // Prevents instantiation

    /**
     * Returns the X coordinate of the longitude on a Mercator projection, given a zoom level.
     *
     * @param zoomLevel the zoom level, should be between 0 and 19
     * @param longitude the longitude in radians
     * @return the X coordinate on a Mercator projection
     */
    public static double x(int zoomLevel, double longitude) {

        /*double a = 1 << (8 + zoomLevel); // 2^(8+zoomLevel)
        double b = (longitude / 2 * Math.PI) + 0.5;
        return a * b;*/
        double a = 256 * Math.pow(2, zoomLevel);
        double x = a * (longitude / (2 * Math.PI)) + a / 2;
        return x;
        //double b = Units.convertTo(longitude, Units.Angle.TURN) + 0.5;
        //return a * b;
    }

    /**
     * Returns the Y coordinate of the latitude on a Mercator projection, given a zoom level.
     *
     * @param zoomLevel the zoom level, should be between 0 and 19
     * @param latitude  the latitude in radians
     * @return the Y coordinate on a Mercator projection
     */
    public static double y(int zoomLevel, double latitude) {
        double a = 1 << (8 + zoomLevel); // 2^(8+zoomLevel)
        double b = Units.convertTo(-Math2.asinh(Math.tan(latitude)), Units.Angle.TURN) + 0.5;
        return a * b;
    }
}
