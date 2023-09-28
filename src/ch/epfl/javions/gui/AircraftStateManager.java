/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.adsb.AircraftStateAccumulator;
import ch.epfl.javions.adsb.Message;
import ch.epfl.javions.aircraft.AircraftData;
import ch.epfl.javions.aircraft.AircraftDatabase;
import ch.epfl.javions.aircraft.IcaoAddress;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static ch.epfl.javions.Units.Time.MINUTE_IN_NS;
import static java.util.Objects.requireNonNull;

/**
 * Represents the manager of the aircraft states. Responsible for updating the state of the aircrafts and for
 * purging the states that have not been updated in the last minute, as well as updating the set of aircraft states.
 */
public final class AircraftStateManager {
    private final Map<IcaoAddress, AircraftStateAccumulator<ObservableAircraftState>> accumulators;
    private final ObservableSet<ObservableAircraftState> states;
    private final ObservableSet<ObservableAircraftState> unmodifiableStates;
    private final AircraftDatabase database;

    //initially set to -1 to avoid purging the states before the first update.
    private long lastTimeStamp = -1;

    /**
     * Constructs a new {@link AircraftStateManager} object with the given database.
     *
     * @param database the database to use to get the aircraft data
     */
    public AircraftStateManager(AircraftDatabase database) {
        this.database = requireNonNull(database);
        this.accumulators = new HashMap<>();
        this.states = FXCollections.observableSet();
        this.unmodifiableStates = FXCollections.unmodifiableObservableSet(states);
    }

    /**
     * Returns the set of aircraft states.
     *
     * @return the unmodifiable set of aircraft states {@link #unmodifiableStates}
     */
    public ObservableSet<ObservableAircraftState> states() {
        return unmodifiableStates;
    }

    /**
     * Updates the state of the aircraft with the given message. If the aircraft is not in the database, it is added to
     * the database. If the aircraft is not in the set of aircraft states, it is added to the set of aircraft states.
     *
     * @param message the message to update the state with
     * @throws IOException if the database cannot be accessed (see {@link java.util.zip.ZipFile#getInputStream}
     * @see AircraftDatabase#get(IcaoAddress) for the format of the returned data.
     */
    public void updateWithMessage(Message message) throws IOException {

        AircraftData data = database.get(message.icaoAddress());
        AircraftStateAccumulator<ObservableAircraftState> stateAcc = accumulators.get(message.icaoAddress());

        if (stateAcc == null) {
            ObservableAircraftState accumulator = new ObservableAircraftState(message.icaoAddress(), data);
            stateAcc = new AircraftStateAccumulator<>(accumulator);
            accumulators.put(message.icaoAddress(), stateAcc);
        }
        lastTimeStamp = message.timeStampNs();
        stateAcc.update(message);

        if (stateAcc.stateSetter().getPosition() != null)
            states.add(stateAcc.stateSetter());
    }

    /**
     * Purges the states from {@link #states} and {@link #accumulators} that have not been updated in the last minute.
     */
    public void purge() {
        Iterator<AircraftStateAccumulator<ObservableAircraftState>> it = accumulators.values().iterator();

        while (it.hasNext()) {
            AircraftStateAccumulator<ObservableAircraftState> state = it.next();
            if (Math.abs(lastTimeStamp - state.stateSetter().getLastMessageTimeStampNs()) > MINUTE_IN_NS) {
                it.remove();
                states.remove(state.stateSetter());
                accumulators.remove(state.stateSetter().getIcaoAddress());
            }
        }
    }
}
