/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.Preconditions;
import javafx.beans.property.*;

/**
 * Represents the parameters of the {@link BaseMapController} object. These parameters are the zoom level of the map
 * and the coordinates of the top left corner of the visible part of the map.
 */
public final class MapParameters {
    public final static int MIN_ZOOM = 6;
    public final static int MAX_ZOOM = 19;
    private final IntegerProperty zoom;
    private final DoubleProperty minX;
    private final DoubleProperty minY;

    /**
     * Constructs a new {@link MapParameters} object with the given zoom level and the given coordinates of the top left
     * corner of the visible part of the map.
     *
     * @param zoom the zoom level of the map
     * @param minX the x coordinate of the top left corner of the visible part of the map
     * @param minY the y coordinate of the top left corner of the visible part of the map
     * @throws IllegalArgumentException if the zoom level is not between {@link #MIN_ZOOM} and {@link #MAX_ZOOM}
     */
    public MapParameters(int zoom, double minX, double minY) {
        Preconditions.checkArgument(zoom >= MIN_ZOOM && zoom <= MAX_ZOOM);

        this.zoom = new SimpleIntegerProperty(zoom);
        this.minX = new SimpleDoubleProperty(minX);
        this.minY = new SimpleDoubleProperty(minY);

    }

    /**
     * Returns the zoom level of the map as a property.
     *
     * @return the zoom level of the map as a property
     */
    public ReadOnlyIntegerProperty getZoomProperty() {
        return zoom;
    }

    /**
     * Returns the x coordinate of the top left corner of the visible part of the map as a property.
     *
     * @return the x coordinate of the top left corner of the visible part of the map as a property.
     */
    public ReadOnlyDoubleProperty getMinXProperty() {
        return minX;
    }

    /**
     * Returns the y coordinate of the top left corner of the visible part of the map as a property.
     *
     * @return the y coordinate of the top left corner of the visible part of the map as a property
     */
    public ReadOnlyDoubleProperty getMinYProperty() {
        return minY;
    }

    /**
     * Returns the zoom level of the map.
     *
     * @return the zoom level of the map
     */
    public int getZoomValue() {
        return zoom.get();
    }

    /**
     * Returns the x coordinate of the top left corner of the visible part of the map.
     *
     * @return the x coordinate of the top left corner of the visible part of the map
     */
    public double getMinXValue() {
        return minX.get();
    }

    /**
     * Returns the y coordinate of the top left corner of the visible part of the map.
     *
     * @return the y coordinate of the top left corner of the visible part of the map
     */
    public double getMinYValue() {
        return minY.get();
    }


    /**
     * Translates the position of the visible part of a map by the given vector.
     *
     * @param dx dx component of the translation vector
     * @param dy dy component of the translation vector
     */
    public void scroll(double dx, double dy) {
        minX.set(minX.get() + dx);
        minY.set(minY.get() + dy);
    }

    /**
     * Updates the {@link #zoom} level, {@link #minX}, and {@link #minY} properties of the map according to
     * the given zoom variation.
     *
     * @param dz the zoom level variation
     */
    public void changeZoomLevel(int dz) {
        //TODO : use Math2.clamp
        int newZoom = zoom.get() + dz;
        //newZoom = Math2.clamp(newZoom, MIN_ZOOM, MAX_ZOOM);
        if (newZoom < MIN_ZOOM) {
            newZoom = MIN_ZOOM;
        } else if (newZoom > MAX_ZOOM) {
            newZoom = MAX_ZOOM;
        }

        double factor = Math.scalb(1, newZoom - zoom.get());

        minX.set(minX.get() * factor);
        minY.set(minY.get() * factor);
        zoom.set(newZoom);
    }
}
