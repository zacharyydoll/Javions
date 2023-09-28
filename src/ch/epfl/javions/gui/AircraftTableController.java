package ch.epfl.javions.gui;

import ch.epfl.javions.GeoPos;
import ch.epfl.javions.Units;
import ch.epfl.javions.adsb.AirbornePositionMessage;
import ch.epfl.javions.adsb.AirborneVelocityMessage;
import ch.epfl.javions.adsb.CallSign;
import ch.epfl.javions.aircraft.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Function;

import static ch.epfl.javions.Units.Angle.DEGREE;
import static ch.epfl.javions.Units.Speed.KILOMETER_PER_HOUR;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS;

/**
 * Represents the table used to display the current states of the aircrafts.
 */
public final class AircraftTableController {

    public final static int ICAO_COLUMN_WIDTH = 60;
    public final static int CALL_SIGN_COLUMN_WIDTH = 70;
    public final static int REG_COLUMN_WIDTH = 90;
    public final static int MODEL_COLUMN_WIDTH = 230;
    public final static int TYPE_COLUMN_WIDTH = 50;
    public final static int DESC_COLUMN_WIDTH = 70;
    public final static int NUMERIC_COLUMN_WIDTH = 85;
    public final static int POSITION_DECIMALS = 4;
    public final static int ALT_SPEED_DECIMALS = 0;
    private final TableView<ObservableAircraftState> table;
    private final MenuItem followMenuItem;
    private boolean isTableCollapsed;
    private boolean isFollowing;
    private ObservableAircraftState selectedState;

    /**
     * Creates a new aircraft table controller.
     *
     * @param observableAircraftStates the set of aircraft states to display
     * @param selectedAircraft         the currently selected aircraft
     * @see ObservableAircraftState for more information about the aircraft states
     */
    public AircraftTableController(ObservableSet<ObservableAircraftState> observableAircraftStates,
                                   ObjectProperty<ObservableAircraftState> selectedAircraft) {

        table = new TableView<>();
        table.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
        table.setTableMenuButtonVisible(true);
        table.getStyleClass().add("table");
        isTableCollapsed = false;
        isFollowing = false;

        ContextMenu contextMenu = new ContextMenu();
        followMenuItem = new MenuItem("Follow");
        contextMenu.getItems().add(followMenuItem);
        table.setContextMenu(contextMenu);

        observableAircraftStates.addListener((SetChangeListener<ObservableAircraftState>) change -> {
            if (change.wasAdded()) {
                table.getItems().add(change.getElementAdded());
                table.sort();

            } else if (change.wasRemoved()) {
                table.getItems().remove(change.getElementRemoved());
            }
        });

        selectedAircraft.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                table.getSelectionModel().select(newValue);

                //if the item is not already visible, scroll to it
                if (!table.selectionModelProperty().get().getSelectedItems().equals(newValue)) {
                    table.scrollTo(newValue);
                    table.getSelectionModel().select(newValue);
                }
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null)
                selectedAircraft.set(newValue);
        });

        configureTextColumns();
        configureNumericalColumns();
        setOnDoubleClick(selectedAircraft::set);
    }

    /**
     * Returns the table view of the aircraft states.
     *
     * @return the {@link TableView} of {@link ObservableAircraftState}
     */
    public TableView<ObservableAircraftState> pane() {
        return table;
    }

    /**
     * Sets the {@link Consumer} to call when an item is double clicked on.
     *
     * @param consumer the consumer to call when an item is double clicked using the primary mouse button
     * @note double clicking on an aircraft will select it and follow it. To unfollow it, click on the "Follow/Unfollow"
     * menu item. Double clicking on another aircraft will unfollow the current one and follow the new one.
     */
    public void setOnDoubleClick(Consumer<ObservableAircraftState> consumer) {
        table.setOnMouseClicked(event -> {
            //accept if double click and non-null item
            if (event.getClickCount() == 2 && event.getButton() == MouseButton.PRIMARY) {
                selectedState = table.getSelectionModel().getSelectedItem();
                if (selectedState != null) {
                    followMenuItem.setText("Follow/Unfollow");
                    isFollowing = true;
                    consumer.accept(selectedState);
                }
            }
        });
    }

    /**
     * Configures the text columns of the {@link #table}, namely the :
     * <ul>
     *     <li>{@link IcaoAddress}, {@link CallSign},{@link AircraftData#registration()},
     *     <li>{@link AircraftData#model()}, {@link AircraftData#typeDesignator()}, {@link AircraftData#description()}
     * </ul>
     * of the aircraft.
     */
    private void configureTextColumns() {
        table.getColumns().addAll(

                createTextColumn("ICAO", ICAO_COLUMN_WIDTH, state ->
                        state.getIcaoAddressProperty().map(IcaoAddress::string)),
                createTextColumn("Call Sign", CALL_SIGN_COLUMN_WIDTH, state ->
                        state.getCallSignProperty().map(CallSign::string)),
                createTextColumn("Registration", REG_COLUMN_WIDTH, state -> {
                    AircraftData ad = state.getAircraftData();
                    return ad == null ?
                            readOnlyValue("") : readOnlyValue(ad.registration()).map(AircraftRegistration::string);
                }),
                createTextColumn("Model", MODEL_COLUMN_WIDTH, state -> {
                    AircraftData ad = state.getAircraftData();
                    return ad == null ?
                            readOnlyValue("") : readOnlyValue(ad.model());
                }),
                createTextColumn("Type", TYPE_COLUMN_WIDTH, state -> {
                    AircraftData ad = state.getAircraftData();
                    return ad == null ? readOnlyValue("") :
                            readOnlyValue(ad.typeDesignator()).map(AircraftTypeDesignator::string);
                }),
                createTextColumn("Description", DESC_COLUMN_WIDTH, state -> {
                    AircraftData ad = state.getAircraftData();
                    return ad == null ?
                            readOnlyValue("") : readOnlyValue(ad.description()).map(AircraftDescription::string);
                })
        );
    }

    /**
     * Creates a {@link TableColumn} with the given title, width and value extractor.
     *
     * @param title          the title of the column
     * @param width          the width of the column
     * @param valueExtractor the function to extract the value of the column
     * @return a {@link TableColumn} with the given title, width and value extractor
     */
    private TableColumn<ObservableAircraftState, String> createTextColumn(
            String title,
            double width,
            Function<ObservableAircraftState, ObservableValue<String>> valueExtractor) {

        TableColumn<ObservableAircraftState, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> valueExtractor.apply(data.getValue()));
        column.setPrefWidth(width);
        return column;
    }

    /**
     * Configures the numerical columns of the {@link #table}, namely the :
     * <ul>
     *     <li> {@link GeoPos#latitude()},{@link GeoPos#longitude()},
     *     <li> {@link AirbornePositionMessage#altitude()} and {@link AirborneVelocityMessage#speed()}
     * </ul>
     * of the aircraft.
     */
    private void configureNumericalColumns() {

        TableColumn<ObservableAircraftState, String> latitudeColumn = createNumericColumn(
                "Latitude (°)",
                POSITION_DECIMALS,
                p -> Units.convertTo(p.getValue().getPosition().latitude(), DEGREE),
                ObservableAircraftState::getLatitudeProperty);

        TableColumn<ObservableAircraftState, String> longitudeColumn = createNumericColumn("Longitude (°)",
                POSITION_DECIMALS,
                p -> Units.convertTo(p.getValue().getPosition().longitude(), DEGREE),
                ObservableAircraftState::getLongitudeProperty);

        TableColumn<ObservableAircraftState, String> altitudeColumn = createNumericColumn("Altitude (m)",
                ALT_SPEED_DECIMALS,
                p -> p.getValue().getAltitude(),
                ObservableAircraftState::getAltitudeProperty);

        TableColumn<ObservableAircraftState, String> velocityColumn = createNumericColumn("Speed (km/h)",
                ALT_SPEED_DECIMALS,
                p -> Units.convertTo(p.getValue().getVelocity(), KILOMETER_PER_HOUR),
                ObservableAircraftState::getVelocityProperty);

        table.getColumns().addAll(longitudeColumn, latitudeColumn, altitudeColumn, velocityColumn);
    }

    /**
     * Creates a numerical column with the given properties
     *
     * @param headerText        the name of the column
     * @param fractionDigits    the number of digits to display after the decimal point
     * @param valueExtractor    a {@link Function} that extracts the value to display from the cell data features
     * @param propertyExtractor a {@link Function} that extracts the property to bind the cell to
     * @return a {@link TableColumn} with the given numerical properties
     */
    private TableColumn<ObservableAircraftState, String> createNumericColumn(
            String headerText, int fractionDigits,
            Function<CellDataFeatures<ObservableAircraftState, String>, Double> valueExtractor,
            Function<ObservableAircraftState, ReadOnlyDoubleProperty> propertyExtractor) {

        NumberFormat format = NumberFormat.getInstance();
        TableColumn<ObservableAircraftState, String> column = new TableColumn<>(headerText);
        column.setCellValueFactory(p -> {
            Double value = valueExtractor.apply(p);

            //if (Double.isNaN(value)) {
            // return new ReadOnlyStringWrapper("");
            //}

            format.setMinimumFractionDigits(fractionDigits);
            format.setMaximumFractionDigits(fractionDigits);
            return Bindings.createStringBinding(() -> format.format(value), propertyExtractor.apply(p.getValue()));
        });

        column.setPrefWidth(NUMERIC_COLUMN_WIDTH);
        column.getStyleClass().add("numeric");
        column.setComparator((s1, s2) -> {
            if (s1.isEmpty() || s2.isEmpty()) {
                return s1.compareTo(s2);
            } else {
                try {
                    return Double.compare(format.parse(s1).doubleValue(), format.parse(s2).doubleValue());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return column;
    }

    /**
     * Wraps the given value in an observable value that is always equal to the given value.
     *
     * @param e   the value to wrap
     * @param <E> the type of the value
     * @return an {@link ObservableValue<E>} value that is always equal to the given value
     */
    private <E> ObservableValue<E> readOnlyValue(E e) {
        return new ReadOnlyObjectWrapper<>(e);
    }

    //BONUS METHODS

    /**
     * Returns a boolean indicating whether the {@link #table} is collapsed or not.
     *
     * @return the boolean value of {@link #isTableCollapsed}
     */
    public boolean isTableCollapsed() {
        return this.isTableCollapsed;
    }

    /**
     * Sets {@link #isTableCollapsed} to the given boolean value
     *
     * @param isTableCollapsed the boolean value to set {@link #isTableCollapsed} to
     */
    public void setTableCollapsed(boolean isTableCollapsed) {
        this.isTableCollapsed = isTableCollapsed;
    }

    /**
     * Sets {@link #isFollowing} to the given boolean value
     *
     * @param isFollowing the boolean value to set {@link #isFollowing} to
     */
    public void setIsFollowing(boolean isFollowing) {
        this.isFollowing = isFollowing;
    }

    /**
     * Returns a boolean indicating whether the map is following a plane or not.
     *
     * @return the boolean value of {@link #isFollowing}
     */
    public boolean isFollowing() {
        return this.isFollowing;
    }

    /**
     * Sets the {@link Consumer} to call when the user clicks on the follow/unfollow button is clicked on.
     * The text of the button is changed accordingly.
     *
     * @param consumer the consumer to call when the user clicks on the follow/unfollow {@link MenuItem}
     * @note the {@link #followMenuItem} is accessible by right-clicking on a plane in the {@link #table}.
     */
    public void setOnFollowClicked(Consumer<ObservableAircraftState> consumer) {
        followMenuItem.setOnAction(e -> {
            selectedState = table.getSelectionModel().getSelectedItem();
            followMenuItem.setText("Follow/Unfollow");
            consumer.accept(selectedState);
        });
    }
}
