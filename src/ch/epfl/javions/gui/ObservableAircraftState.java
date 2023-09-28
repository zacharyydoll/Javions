/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.GeoPos;
import ch.epfl.javions.adsb.AirborneVelocityMessage;
import ch.epfl.javions.adsb.AircraftStateSetter;
import ch.epfl.javions.adsb.CallSign;
import ch.epfl.javions.aircraft.AircraftData;
import ch.epfl.javions.aircraft.IcaoAddress;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import static java.lang.Double.NaN;


/**
 * Immutable class representing the state of an aircraft.
 */
public final class ObservableAircraftState implements AircraftStateSetter {

    private final AircraftData data;
    private final IcaoAddress icaoAddress;
    private final LongProperty lastMessageTimeStampNs = new SimpleLongProperty();
    private final IntegerProperty category = new SimpleIntegerProperty();
    private final ObjectProperty<CallSign> callSign = new SimpleObjectProperty<>();
    private final ObjectProperty<GeoPos> position = new SimpleObjectProperty<>();
    private final ObservableList<AirbornePos> trajectory = FXCollections.observableArrayList();
    private final DoubleProperty trackOrHeading = new SimpleDoubleProperty();
    private final DoubleProperty latitude = new SimpleDoubleProperty();
    private final DoubleProperty longitude = new SimpleDoubleProperty();

    //initially set to NaN because we don't know the velocity of the aircraft.
    private final DoubleProperty altitude = new SimpleDoubleProperty(NaN);
    private final DoubleProperty velocity = new SimpleDoubleProperty(NaN);
    private long lastTimeStamp = -1;


    /**
     * Constructs a new ObservableAircraftState for an aircraft with the given Icao address and data.
     *
     * @param icaoAddress the Icao address of the aircraft
     * @param data        the data of the aircraft
     */
    public ObservableAircraftState(IcaoAddress icaoAddress, AircraftData data) {
        this.icaoAddress = icaoAddress;
        this.data = data;
    }

    /**
     * Returns the {@link AircraftData} of an aircraft.
     *
     * @return the aircraft data of an aircraft
     */
    public AircraftData getAircraftData() {
        return data;
    }

    public ReadOnlyObjectProperty<AircraftData> getAircraftDataProperty() {
        return new SimpleObjectProperty<>(this.data);
    }

    /**
     * Returns the {@link IcaoAddress} of an aircraft.
     *
     * @return the Icao address of an aircraft
     */
    public IcaoAddress getIcaoAddress() {
        return this.icaoAddress;
    }

    /**
     * Returns the Icao address of an aircraft as a {@link SimpleObjectProperty}.
     *
     * @return the Object property of the Icao address of an aircraft
     */
    public SimpleObjectProperty<IcaoAddress> getIcaoAddressProperty() {
        return new SimpleObjectProperty<>(this.icaoAddress);
    }


    //============================================================================================
    //=================================== TimeStamp ==============================================
    //============================================================================================


    /**
     * Returns the {@link #lastMessageTimeStampNs} of an aircraft.
     *
     * @return the time stamp of the last message of an aircraft
     */
    public long getLastMessageTimeStampNs() {
        return lastMessageTimeStampNs.get();
    }

    /**
     * Sets the {@link #lastMessageTimeStampNs} of an aircraft to a given value.
     *
     * @param timeStampNs the new timestamp value in nanoseconds to be set to
     */
    @Override
    public void setLastMessageTimeStampNs(long timeStampNs) {
        this.lastMessageTimeStampNs.set(timeStampNs);
    }

    /**
     * Returns the {@link #lastMessageTimeStampNs} of an aircraft as a ReadOnlyLongProperty.
     *
     * @return the timeStamp of an aircraft as a ReadOnlyLongProperty.
     */
    public ReadOnlyLongProperty lastMessageTimeStampNsProperty() {
        return lastMessageTimeStampNs;
    }


    //============================================================================================
    //=================================== Category ===============================================
    //============================================================================================


    /**
     * Returns the category of an aircraft.
     *
     * @return the category of an aircraft.
     */
    public int getCategory() {
        return category.intValue();
    }

    /**
     * Sets the {@link #category} of an aircraft to a given value.
     *
     * @param category the new category value
     */
    @Override
    public void setCategory(int category) {
        this.category.set(category);
    }

    /**
     * Returns the category of an aircraft as a ReadOnlyIntegerProperty.
     *
     * @return the {@link #category} of an aircraft.
     */
    public ReadOnlyIntegerProperty categoryProperty() {
        return category;
    }


    //============================================================================================
    //=================================== CallSign ===============================================
    //============================================================================================


    /**
     * Returns the call sign of an aircraft
     *
     * @return the call sign of an aircraft
     */
    public CallSign getCallSign() {
        return callSign.get();
    }

    /**
     * Sets the {@link CallSign} of an aircraft to a given value.
     *
     * @param callSign the new Call sign value
     */
    @Override
    public void setCallSign(CallSign callSign) {
        this.callSign.set(callSign);
    }

    /**
     * Returns the call sign of an aircraft as a {@link CallSign} object.
     *
     * @return the {@link #callSign} of an aircraft.
     */
    public ReadOnlyObjectProperty<CallSign> getCallSignProperty() {
        return callSign;
    }


    //============================================================================================
    //=================================== Position ===============================================
    //============================================================================================


    public GeoPos getPosition() {
        return position.get();
    }

    /**
     * Sets the position of an aircraft to a given value.
     *
     * @param position the new position value as a {@link GeoPos} object
     */
    @Override
    public void setPosition(GeoPos position) {
        this.position.set(position);
        this.latitude.set(position.latitude());
        this.longitude.set(position.longitude());
        updateTrajectory();
    }


    /**
     * Returns the latitude of an aircraft as a ReadOnlyDoubleProperty.
     *
     * @return the {@link #latitude} property of an aircraft.
     */

    public ReadOnlyDoubleProperty getLatitudeProperty() {
        return latitude;
    }

    /**
     * Returns the longitude of an aircraft as a ReadOnlyDoubleProperty.
     *
     * @return the {@link #longitude} property of an aircraft.
     */
    public ReadOnlyDoubleProperty getLongitudeProperty() {
        return longitude;
    }

    /**
     * Returns the position of an aircraft as a ReadOnlyObjectProperty.
     *
     * @return the {@link #position} property of an aircraft.
     */
    public ReadOnlyObjectProperty<GeoPos> getPositionProperty() {
        return position;
    }


    //============================================================================================
    //=================================== Altitude ===============================================
    //============================================================================================


    /**
     * Returns the altitude of an aircraft in meters.
     *
     * @return the {@link #altitude} of an aircraft in meters.
     */
    public double getAltitude() {
        return altitude.get();
    }

    /**
     * Sets the altitude of an aircraft to a given value.
     *
     * @param altitude the new altitude value in meters
     */
    @Override
    public void setAltitude(double altitude) {
        this.altitude.set(altitude);
        updateTrajectory();
    }

    /**
     * Returns the altitude property of an aircraft.
     *
     * @return the {@link #altitude} property of an aircraft.
     */
    public ReadOnlyDoubleProperty getAltitudeProperty() {
        return altitude;
    }


    //============================================================================================
    //=================================== Velocity ===============================================
    //============================================================================================


    /**
     * Returns the velocity of the aircraft in meters per second.
     *
     * @return the velocity of the aircraft in meters per second
     */
    public double getVelocity() {
        return velocity.get();
    }

    /**
     * Sets the velocity of an aircraft to a given value.
     *
     * @param velocity the new velocity value in meters per second
     */
    @Override
    public void setVelocity(double velocity) {
        this.velocity.set(velocity);
    }

    /**
     * Returns the velocity property of an aircraft in meters per second.
     *
     * @return the {@link #velocity} property of an aircraft
     */
    public ReadOnlyDoubleProperty getVelocityProperty() {
        return velocity;
    }


    //============================================================================================
    //=================================== TrackOrHeading =========================================
    //============================================================================================


    /**
     * Returns the trackOrHeading of an aircraft.
     *
     * @return the {@link #trackOrHeading} of an aircraft as a double
     * @see AirborneVelocityMessage#trackOrHeading()
     */
    public double getTrackOrHeading() {
        return trackOrHeading.get();
    }

    /**
     * Sets the trackOrHeading of an aircraft to a given value.
     *
     * @param trackOrHeading the new track or heading value in radians
     */
    @Override
    public void setTrackOrHeading(double trackOrHeading) {
        this.trackOrHeading.set(trackOrHeading);
    }

    /**
     * Returns the trackOrHeading property of an aircraft.
     *
     * @return the {@link #trackOrHeading} property of an aircraft.
     */
    public ReadOnlyDoubleProperty getTrackOrHeadingProperty() {
        return trackOrHeading;
    }


    //============================================================================================
    //=================================== Trajectory =============================================
    //============================================================================================


    /**
     * Returns the trajectory list of an aircraft.
     *
     * @return the trajectory of the aircraft, as an {@link ObservableList} of {@link AirbornePos} records.
     */
    public ObservableList<AirbornePos> getTrajectory() {
        return trajectory;
    }

    /**
     * Updates the trajectory list with the current position and altitude, if the position or altitude
     * have changed.
     * <p>
     * If the position has changed, a new {@link AirbornePos} is added to the list. If the position
     * has not changed but the altitude has, the last position is updated with the new altitude.
     *
     * @see AirbornePos for details on how the {@link #trajectory} is stored.
     */
    private void updateTrajectory() {
        GeoPos currentPos = position.get();
        double currentAltitude = getAltitude();
        long currentTimeStampNs = lastMessageTimeStampNs.get();

        if (currentPos == null || Double.isNaN(currentAltitude))
            return;

        boolean posChanged = !trajectory.isEmpty() &&
                !currentPos.equals(trajectory.get(trajectory.size() - 1).position());

        if (posChanged || trajectory.isEmpty()) {
            trajectory.add(new AirbornePos(currentPos, currentAltitude));
            lastTimeStamp = currentTimeStampNs;

        } else if (currentTimeStampNs == lastTimeStamp &&
                currentAltitude != trajectory.get(trajectory.size() - 1).altitude()) {
            trajectory.set(trajectory.size() - 1, new AirbornePos(currentPos, currentAltitude));
            lastTimeStamp = currentTimeStampNs;
        }
    }


    /**
     * A record that stores the current position and altitude of an aircraft.
     *
     * @param position the current position of the aircraft
     * @param altitude the current altitude of the aircraft
     */
    public record AirbornePos(GeoPos position, double altitude) {
    }
}
