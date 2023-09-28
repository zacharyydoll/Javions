package ch.epfl.javions;


/**
 * This class defines standard quantities, that serve as a reference for other quantities to be expressed in.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class Units {

    /**
     * The Centi prefix, defined as 10^-2.
     */
    public final static double CENTI = 1e-2;
    /**
     * The Kilo prefix, defined as 10^3.
     */
    public final static double KILO = 1e3;

    /**
     * The Nano prefix, defined as 10^-9.
     */
    public static final double NANO = 1e-9;

    /**
     * The Milli prefix, defined as 10^-3.
     */
    public static final double MILLI = 1e-3;

    private Units() {
    } // Prevents instantiation

    /**
     * Converts an inputted value from one unit to another.
     *
     * @param value    the value we want to convert, expected to represent the same quantity as fromUnit
     * @param fromUnit the value's current unit
     * @param toUnit   the unit we wish to convert the value to
     * @return the value converted to the new unit
     * @note The value and both units are expected to represent the same quantity.
     */
    public static double convert(double value, double fromUnit, double toUnit) {
        return value * (fromUnit / toUnit);
    }

    /**
     * Converts the inputted value to its base unit form.
     *
     * @param value    value we wish to convert to its base unit form
     * @param fromUnit the current unit of the inserted value
     * @return The value converted to its base unit form
     * @note The value and unit are expected to represent the same quantity.
     */
    public static double convertFrom(double value, double fromUnit) {
        return convert(value, fromUnit, 1);
    }

    /**
     * Converts the inputted value from its base unit form to the new unit, while conserving the quantity it represents.
     *
     * @param value  value we wish to convert, expected to be in base unit form
     * @param toUnit unit we wish to convert the value to
     * @return the value converted from its base unit form to the new unit
     * @note The value and unit are expected to represent the same quantity.
     */
    public static double convertTo(double value, double toUnit) {
        return convert(value, 1, toUnit);
    }

    /**
     * Definitions of the different units of Angles, using the Radian as the base unit.
     */
    public static class Angle {
        /**
         * The RADIAN, base unit.
         */
        public final static double RADIAN = 1;
        /**
         * The TURN, represents a full revolution of a circle.
         */
        public final static double TURN = RADIAN * (2 * Math.PI);
        /**
         * The DEGREE, defined to be PI*RAD/180.
         */
        public final static double DEGREE = TURN / 360;
        /**
         * The T32, defined as a TURN divided by 2^32.
         */
        public final static double T32 = TURN / Math.scalb(1, 32);

        private Angle() {
        } // Prevents instantiation
    }

    /**
     * Definitions of the different units of length, using the Meter as the base unit.
     */
    public static class Length {
        /**
         * The METER, base unit.
         */
        public final static double METER = 1;
        /**
         * The CENTIMETER, defined as a hundredth of a meter.
         */
        public final static double CENTIMETER = METER * CENTI;
        /**
         * The INCH, a unit used in the imperial system, defined as 2.54 centimeters.
         */
        public final static double INCH = CENTIMETER * 2.54;
        /**
         * The FOOT, a unit used in the imperial system, defined as 12 inches.
         */
        public final static double FOOT = INCH * 12;
        /**
         * The kilometer, defined as a thousand meters.
         */
        public final static double KILOMETER = METER * KILO;
        /**
         * The NAUTICAL MILE, a unit used in the imperial system, defined as 1852 meters.
         */
        public final static double NAUTICAL_MILE = Length.METER * 1852;

        private Length() {
        }
    }

    /**
     * Definitions of the different units of time, using the second as the base unit.
     */
    public static class Time {
        /**
         * The SECOND, base unit.
         */
        public final static double SECOND = 1;


        /**
         * The NANO SECOND, defined as a second divided by 10^9.
         */
        public final static double NANO_SECOND = SECOND * NANO;

        /*
         * The MILLI SECOND, defined as a second divided by 10^3.
         */
        public final static double MILLI_SECOND = SECOND * MILLI;

        /**
         * The MINUTE, defined as 60 seconds.
         */
        public final static double MINUTE = 60 * SECOND;

        /**
         * The MINUTE, defined as 60 seconds, in nanoseconds.
         */
        public final static double MINUTE_IN_NS = MINUTE / NANO;
        /**
         * The HOUR, defined as 60 minutes.
         */
        public final static double HOUR = 60 * MINUTE;

        private Time() {
        } // Prevents instantiation
    }

    /**
     * Definitions of the different units of speed, using meters per second as the base unit.
     */
    public static class Speed {
        /**
         * METERS PER SECOND, base unit.
         */
        public final static double METERS_PER_SECOND = Length.METER / Time.SECOND;
        /**
         * the KNOT, defined as nautical miles per hour.
         */
        public final static double KNOT = Length.NAUTICAL_MILE / Time.HOUR;
        /**
         * KILOMETER PER HOUR, defined as kilometers per hour.
         */
        public final static double KILOMETER_PER_HOUR = Length.KILOMETER / Time.HOUR;

        private Speed() {
        } // Prevents instantiation
    }
}
