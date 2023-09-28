/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;

/**
 * Class that handles the status line of the application.
 */
public final class StatusLineController {

    public final static int MAX_SLIDER_ALTITUDE = 10_000;
    public final static int SLIDER_TICK_UNIT = 2000;
    public final static int SLIDER_BLOCK_INCREMENT = 1000;
    private final BorderPane pane;
    private final IntegerProperty aircraftCountProperty;
    private final LongProperty messageCountProperty;
    //BONUS
    private final TextField searchField;
    private final Slider altitudeSlider;


    /**
     * Creates a new {@link StatusLineController} object.
     */
    public StatusLineController() {

        this.pane = new BorderPane();
        pane.getStylesheets().add("status.css");

        searchField = new TextField();
        pane.setCenter(searchField);

        altitudeSlider = new Slider(0, MAX_SLIDER_ALTITUDE, 0); // min: 0, max: 12000, initial value: 0
        altitudeSlider.setShowTickLabels(true);
        altitudeSlider.setShowTickMarks(true);
        altitudeSlider.setMajorTickUnit(SLIDER_TICK_UNIT);
        altitudeSlider.setBlockIncrement(SLIDER_BLOCK_INCREMENT);

        pane.setBottom(altitudeSlider);

        Text aircraftCountText = new Text("Nb. of visible aircrafts: ");
        Text messageCountText = new Text("Nb. of messages received: ");

        //Changed the positions of the texts on the pane to make space for the search bar.
        pane.setLeft(aircraftCountText);
        pane.setTop(messageCountText);

        aircraftCountProperty = new SimpleIntegerProperty(0);
        messageCountProperty = new SimpleLongProperty(0);

        aircraftCountText.textProperty().bind(aircraftCountProperty.asString("Aéronefs visibles : %d"));
        messageCountText.textProperty().bind(messageCountProperty.asString("Messages reçus : %d"));
    }

    /**
     * Returns the {@link BorderPane} of the status line controller.
     *
     * @return the {@link #pane} of the status line controller
     */
    public BorderPane pane() {
        return this.pane;
    }

    /**
     * Returns the amount of aircrafts that are can currently be seen on the map, including those outside of the visible
     * area.
     *
     * @return the {@link #aircraftCountProperty} of the status line controller
     */
    public IntegerProperty aircraftCountProperty() {
        return aircraftCountProperty;
    }

    /**
     * Returns the total amount of messages that have been received by the application.
     *
     * @return the {@link #messageCountProperty} of the status line controller
     */
    public LongProperty messageCountProperty() {
        return messageCountProperty;
    }

    /**
     * Returns the TextField that acts as a search bar.
     *
     * @return the {@link #searchField} of the status line controller
     */
    public TextField getSearchField() {
        return searchField;
    }

    /**
     * Sets the {@link AircraftController} as a listener to the {@link #altitudeSlider}
     *
     * @param ac the aircraft controller to which the slider is linked
     */
    public void setSliderListener(AircraftController ac) {
        altitudeSlider.valueProperty().addListener((obs, oldVal, newVal) -> ac.setSliderValue(newVal.doubleValue()));
    }
}