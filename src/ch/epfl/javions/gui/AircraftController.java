/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.WebMercator;
import ch.epfl.javions.aircraft.AircraftData;
import ch.epfl.javions.aircraft.AircraftDescription;
import ch.epfl.javions.aircraft.AircraftTypeDesignator;
import ch.epfl.javions.aircraft.WakeTurbulenceCategory;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import static ch.epfl.javions.Units.Angle.DEGREE;
import static ch.epfl.javions.Units.Speed.KILOMETER_PER_HOUR;
import static ch.epfl.javions.Units.convertTo;
import static ch.epfl.javions.gui.ObservableAircraftState.AirbornePos;
import static java.util.Objects.requireNonNull;
import static javafx.scene.paint.CycleMethod.NO_CYCLE;

public final class AircraftController {

    public static final int VISIBLE_ZOOM_LEVEL = 11;
    public static final int MAX_ALTITUDE = 12_000; //in meters
    public final static double POWER_FOR_COLOR = 1.0 / 3.0;
    private final Pane pane;
    private final MapParameters mapParameters;
    private final ObjectProperty<ObservableAircraftState> selectedAircraft;

    //BONUS
    private final DoubleProperty sliderValue = new SimpleDoubleProperty();

    /**
     * Constructs a new AircraftController with the given parameters.
     *
     * @param mapParameters    the {@link MapParameters} of the map
     * @param aircraftStates   the {@link ObservableSet} of {@link ObservableAircraftState} to be displayed
     * @param selectedAircraft the {@link SimpleObjectProperty} of the selected {@link ObservableAircraftState}
     */
    public AircraftController(MapParameters mapParameters,
                              ObservableSet<ObservableAircraftState> aircraftStates,
                              ObjectProperty<ObservableAircraftState> selectedAircraft) {

        this.mapParameters = mapParameters;
        this.selectedAircraft = selectedAircraft;
        //ObservableSet<ObservableAircraftState> aircraftStates1 = unmodifiableObservableSet(aircraftStates);
        this.pane = new Pane();

        pane.getStylesheets().add("aircraft.css");
        pane.setPickOnBounds(false);

        //adding or removing aircrafts from the pane when they are added or removed from the set
        aircraftStates.addListener((SetChangeListener<ObservableAircraftState>) change -> {
            if (change.wasAdded()) {
                Group completeAircraft = completeAircraft(change.getElementAdded());
                completeAircraft.viewOrderProperty().bind(change.getElementAdded().getAltitudeProperty().negate());
                pane.getChildren().add(completeAircraft);
            } else if (change.wasRemoved()) {
                ObservableAircraftState removedAircraft = change.getElementRemoved();
                pane.getChildren().removeIf(node -> node.getId().equals(removedAircraft.getIcaoAddress().string()));
            }
        });
    }

    /**
     * Returns the pane containing the aircraft icons, their labels and their trajectories.
     *
     * @return the {@link #pane} containing the aircraft icons, their labels and their trajectories
     */
    public Pane pane() {
        return pane;
    }

    /**
     * Returns the SVGPath representing the icon of an aircraft
     *
     * @param state the {@link ObservableAircraftState} of the aircraft to be displayed
     * @return a {@link SVGPath} representing the icon of the aircraft
     * @see #getColorFromAltitude for information about the color of the icon
     */
    private SVGPath icon(ObservableAircraftState state) {

        var data = state.getAircraftData();
        var category = state.getCategory();
        var icon = createIcon(data, category);
        var output = new SVGPath();

        configureIcon(icon, output, state);

        // Binding color to altitude
        output.fillProperty().bind(Bindings.createObjectBinding(() ->
                getColorFromAltitude(state.getAltitude()), state.getAltitudeProperty()));

        output.getStyleClass().add("aircraft");

        // Update the icon if the category changes
        state.categoryProperty().addListener((obs, oldCat, newCat) -> {
            if (!(oldCat.equals(newCat))) {
                AircraftIcon newIcon = createIcon(data, (Integer) newCat);
                configureIcon(newIcon, output, state);
            }
        });
        return output;
    }

    /**
     * Configures the rotation property of the aircraft icon.
     *
     * @param icon    the {@link AircraftIcon} of the aircraft
     * @param svgPath the {@link SVGPath} representing the icon of the aircraft
     * @param state   the {@link ObservableAircraftState} of the aircraft
     */
    private void configureIcon(AircraftIcon icon, SVGPath svgPath, ObservableAircraftState state) {
        svgPath.setContent(icon.svgPath());

        if (icon.canRotate()) {
            svgPath.rotateProperty().bind(Bindings.createDoubleBinding(() ->
                    convertTo(state.getTrackOrHeading(), DEGREE), state.getTrackOrHeadingProperty()));
        } else svgPath.setRotate(0);
    }

    /**
     * Returns an Aircraft Icon based on the aircraft's data and category.
     *
     * @param data     the {@link AircraftData}
     * @param category the category of the aircraft
     * @return the {@link AircraftIcon} corresponding to the aircraft
     */
    private AircraftIcon createIcon(AircraftData data, Integer category) {
        try {
            return AircraftIcon.iconFor(
                    data.typeDesignator(),
                    requireNonNull(data.description()),
                    category,
                    requireNonNull(data.wakeTurbulenceCategory())
            );
        } catch (NullPointerException e) {
            return AircraftIcon.iconFor(
                    new AircraftTypeDesignator(""),
                    new AircraftDescription(""),
                    category,
                    WakeTurbulenceCategory.of("")
            );
        }
    }

    /**
     * Returns the label of an aircraft, containing its registration, speed and altitude.
     *
     * @param state the {@link ObservableAircraftState} of the aircraft
     * @return the label of the aircraft as a {@link Group} of {@link Text}s within a {@link Rectangle}
     */
    private Group label(ObservableAircraftState state) {

        //Priority order : registration, call sign, icao address
        StringBinding idBinding = Bindings.createStringBinding(() -> {
            if (state.getAircraftData() != null && state.getAircraftData().registration() != null) {
                return state.getAircraftData().registration().string();
            } else if (state.getCallSign() != null) {
                return state.getCallSign().string();
            } else
                return state.getIcaoAddress().string();
        }, state.getAircraftDataProperty(), state.getCallSignProperty(), state.getIcaoAddressProperty());

        Text regText = new Text();
        regText.textProperty().bind(idBinding);

        Text speedAltitudeText = new Text();

        //Bind speed and altitude to the text, replacing with "?" if speed or altitude are unknown.
        speedAltitudeText.textProperty().bind(Bindings.createStringBinding(() -> {
            String velocityString = Double.isNaN(state.getVelocity()) ? "?km/h" :
                    String.format("%.0fkm/h", convertTo(state.getVelocity(), KILOMETER_PER_HOUR));
            String altitudeString = Double.isNaN(state.getAltitude()) ? "?m" :
                    String.format("%.0fm", state.getAltitude());

            return String.format("\n%s\u2002%s", velocityString, altitudeString);
        }, state.getVelocityProperty(), state.getAltitudeProperty()));

        Rectangle rectangle = new Rectangle();
        rectangle.widthProperty().bind(speedAltitudeText.layoutBoundsProperty().map(b -> b.getWidth() + 4));
        rectangle.heightProperty().bind(speedAltitudeText.layoutBoundsProperty().map(b -> b.getHeight() + 4));

        Group opLabel = new Group(rectangle, regText, speedAltitudeText);
        opLabel.getStyleClass().add("label");

        return opLabel;
    }

    /**
     * Returns a group containing the Lines that make up an aircraft's trajectory.
     *
     * @param state the state of the aircraft
     * @return the trajectory of the aircraft as a {@link Group} of {@link Line}s
     * @see ObservableAircraftState#getTrajectory() and {@link #getColorFromAltitude} for information about the
     * trajectory and its coloration.
     */
    private Group trajectory(ObservableAircraftState state) {
        Group opTrajectory = new Group();

        opTrajectory.layoutXProperty().bind(mapParameters.getMinXProperty().negate());
        opTrajectory.layoutYProperty().bind(mapParameters.getMinYProperty().negate());

        state.getTrajectory().addListener((ListChangeListener<AirbornePos>) change ->
                drawTrajectory(state, opTrajectory));

        mapParameters.getZoomProperty().addListener((observable, oldV, newV) ->
                drawTrajectory(state, opTrajectory));

        opTrajectory.getStyleClass().add("trajectory");

        return opTrajectory;
    }

    /**
     * Returns a group combining the icon and label of an aircraft and positions it at its correct position on a map.
     *
     * @param state the ObservableState of the aircraft
     * @return the {@link Group} containing the {@link #icon(ObservableAircraftState)} and
     * the {@link #label(ObservableAircraftState)} of the aircraft
     * @see ObservableAircraftState#getPosition() for more information on the positionning of this group on the map.
     */
    private Group labelAndIcon(ObservableAircraftState state) {

        SVGPath icon = icon(state);
        Group label = label(state);
        Group output = new Group(icon, label);

        label.setVisible(false);
        icon.setOnMouseClicked(e -> {
            if (selectedAircraft.get() == state)
                selectedAircraft.set(null);
            else
                selectedAircraft.set(state);
            e.consume();
        });

        //Binding visibility to icon selection and zoom level
        ObservableBooleanValue visible = mapParameters.getZoomProperty().greaterThanOrEqualTo(VISIBLE_ZOOM_LEVEL);
        label.visibleProperty().bind(selectedAircraft.isEqualTo(state).or(visible));

        //Binding icon's position on the map to with the aircraft's position and map parameters.
        output.layoutXProperty().bind(Bindings.createDoubleBinding(() ->
                        WebMercator.x(mapParameters.getZoomValue(), state.getPosition().longitude())
                                - mapParameters.getMinXValue(),
                state.getPositionProperty(), mapParameters.getZoomProperty(), mapParameters.getMinXProperty()));

        output.layoutYProperty().bind(Bindings.createDoubleBinding(() ->
                        WebMercator.y(mapParameters.getZoomValue(), state.getPosition().latitude())
                                - mapParameters.getMinYValue(),
                state.getPositionProperty(), mapParameters.getZoomProperty(), mapParameters.getMinYProperty()));

        return output;
    }

    /**
     * Returns a group containing the icon, label and trajectory of an aircraft at its correct position on a map.
     *
     * @param state the {@link ObservableAircraftState} of the aircraft
     * @return the {@link Group} containing the {@link #labelAndIcon(ObservableAircraftState)} and
     * the {@link #trajectory(ObservableAircraftState)} of the aircraft
     */
    private Group completeAircraft(ObservableAircraftState state) {
        Group trajectory = trajectory(state);
        Group labelAndIcon = labelAndIcon(state);

        labelAndIcon.setOnMouseClicked(e -> {
            if (selectedAircraft.get() == state)
                selectedAircraft.set(null);
            else
                selectedAircraft.set(state);
        });

        Group completeAircraft = new Group(trajectory, labelAndIcon);
        completeAircraft.setId(state.getIcaoAddress().string());

        //make the aircraft visible only if their altitude is greater than the slider value
        completeAircraft.visibleProperty().bind(state.getAltitudeProperty().greaterThanOrEqualTo(sliderValue));
        trajectory.visibleProperty().bind(selectedAircraft.isEqualTo(state));

        return completeAircraft;
    }

    /**
     * Returns a color as a function of the altitude of an aircraft.
     *
     * @param altitude the altitude of the aircraft to associate with a color
     * @return the {@link Color} corresponding to the altitude of the aircraft
     * @see ColorRamp#at for information on associating a color with a double value.
     */
    private Color getColorFromAltitude(double altitude) {
        var colors = ColorRamp.PLASMA;
        //formula from the handout
        double ratio = Math.pow(altitude / MAX_ALTITUDE, POWER_FOR_COLOR);
        return colors.at(ratio);
    }

    private void drawTrajectory(ObservableAircraftState state, Group opTrajectory) {
        opTrajectory.getChildren().clear();

        if (state.getTrajectory().isEmpty()) {
            return;
        }

        AirbornePos firstPos = state.getTrajectory().get(0);
        double initAlt = firstPos.altitude();
        if (firstPos.position() == null) {
            return;
        }

        //Computing the coordinates of the start of the line on the map.
        double startX = WebMercator.x(mapParameters.getZoomValue(), firstPos.position().longitude());
        double startY = WebMercator.y(mapParameters.getZoomValue(), firstPos.position().latitude());

        for (AirbornePos airbornePos : state.getTrajectory()) {

            double currAlt = airbornePos.altitude();

            //Computing the coordinates of the end of the line on the map.
            double endX = WebMercator.x(mapParameters.getZoomValue(), airbornePos.position().longitude());
            double endY = WebMercator.y(mapParameters.getZoomValue(), airbornePos.position().latitude());
            Line initLine = new Line(startX, startY, endX, endY);

            //If the altitude changes, the line is drawn in a different color.
            if (!(currAlt + initAlt == 0)) {

                //Computing the color of the line
                var beginColor = getColorFromAltitude(initAlt);
                var endColor = getColorFromAltitude(currAlt);

                initLine.setStroke(new LinearGradient(
                        initLine.getStartX(), initLine.getStartY(),
                        initLine.getEndX(), initLine.getEndY(),
                        true, NO_CYCLE,
                        new Stop(0, beginColor),
                        new Stop(1, endColor))
                );

                initAlt = currAlt;
                initLine.setStroke(getColorFromAltitude(initAlt));

                startX = endX;
                startY = endY;

                //if the altitude is the same, the line is drawn in the same color as the previous one.
            } else {
                initLine.setEndX(endX);
                initLine.setEndY(endY);
            }
            //Adding the newly created line to the group
            opTrajectory.getChildren().add(initLine);
        }
    }

    /**
     * BONUS METHOD, Sets the {@link #sliderValue} to the given altitude.
     *
     * @param altitude the altitude to filter the planes by
     */
    public void setSliderValue(double altitude) {
        this.sliderValue.set(altitude);
    }
}
