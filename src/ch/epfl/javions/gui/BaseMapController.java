package ch.epfl.javions.gui;

import ch.epfl.javions.GeoPos;
import ch.epfl.javions.WebMercator;
import ch.epfl.javions.gui.TileManager.TileId;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * Represents the controller of the map of the world that the {@link AircraftController#pane()} will be stacked
 * on top of.
 */
public final class BaseMapController {

    public static final int SCROLL_DELTA = 200;
    private final TileManager tileManager;
    private final MapParameters mapParameters;
    private final Canvas canvas;
    private final GraphicsContext graphics;
    private final Pane pane;
    private boolean redrawNeeded;
    private Point2D lastMousePos;


    /**
     * Creates a new BaseMapController, which will display the map defined by the given {@link MapParameters}.
     *
     * @param tileManager   the {@link TileManager} used to retrieve the tiles
     * @param mapParameters the {@link MapParameters} used to define the visible part of the map
     */
    public BaseMapController(TileManager tileManager, MapParameters mapParameters) {
        this.mapParameters = mapParameters;
        this.tileManager = tileManager;
        this.redrawNeeded = false;

        canvas = new Canvas();
        graphics = canvas.getGraphicsContext2D();
        pane = new Pane(canvas);

        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());

        canvas.sceneProperty().addListener((p, oldScene, newScene) -> {
            assert oldScene == null && newScene != null;
            newScene.addPreLayoutPulseListener(this::redrawIfNeeded);
        });

        LongProperty minScrollTime = new SimpleLongProperty();
        pane.setOnScroll(e -> {
            int zoomDelta = (int) Math.signum(e.getDeltaY());
            if (zoomDelta == 0) return;

            long currentTime = System.currentTimeMillis();
            if (currentTime < minScrollTime.get()) return;
            minScrollTime.set(currentTime + SCROLL_DELTA);

            mapParameters.scroll(e.getX(), e.getY());
            mapParameters.changeZoomLevel(zoomDelta);
            mapParameters.scroll(-e.getX(), -e.getY());

            redrawOnNextPulse();
            e.consume();
        });
        //memorizing the last mouse position when pressed and redrawing the map
        canvas.setOnMousePressed(e -> {
            lastMousePos = new Point2D(e.getX(), e.getY());
            e.consume();
        });

        //memorizing the last mouse position when dragging and redrawing the map
        canvas.setOnMouseDragged(e -> {
            if (lastMousePos != null) {

                double dx = lastMousePos.getX() - e.getX();
                double dy = lastMousePos.getY() - e.getY();

                mapParameters.scroll(dx, dy);

                lastMousePos = new Point2D(e.getX(), e.getY());
                redrawOnNextPulse();
                e.consume();
            }
        });

        ChangeListener<Object> redrawListener = (observable, oldValue, newValue) -> redrawOnNextPulse();

        canvas.widthProperty().addListener(redrawListener);
        canvas.heightProperty().addListener(redrawListener);

        canvas.sceneProperty().addListener((p, oldScene, newScene) -> {
            assert oldScene == null && newScene != null;
            newScene.addPreLayoutPulseListener(this::redrawIfNeeded);
        });
    }

    /**
     * Returns the {@link #pane} containing the map.
     *
     * @return the {@link Pane} containing the map
     */
    public Pane pane() {
        return pane;
    }

    /**
     * Centers the map on a given {@link GeoPos}.
     *
     * @param pos the {@link GeoPos} to center the map on
     */
    public void centerOn(GeoPos pos) {
        double dx = WebMercator.x(mapParameters.getZoomValue(), pos.longitude()) - mapParameters.getMinXValue();
        double dy = WebMercator.y(mapParameters.getZoomValue(), pos.latitude()) - mapParameters.getMinYValue();

        mapParameters.scroll(dx, dy);
        mapParameters.scroll(-canvas.getWidth() / 2, -canvas.getHeight() / 2);

        redrawOnNextPulse();
    }

    /**
     * Redraws the map if required.
     *
     * @see TileManager#imageForTileAt(TileId) for information on how images are retrieved to draw the map.
     */
    private void redrawIfNeeded() {

        if (!redrawNeeded) return;
        redrawNeeded = false;

        var zoom = mapParameters.getZoomValue();
        var minX = mapParameters.getMinXValue();
        var minY = mapParameters.getMinYValue();
        var tileSize = TileId.TILE_SIZE;

        //Drawing all the tiles that are visible on the canvas (not just one for the visible area)
        int minTileX = (int) Math.floor(minX / tileSize);
        int minTileY = (int) Math.floor(minY / tileSize);
        int maxTileX = (int) Math.ceil((minX + canvas.getWidth()) / tileSize);
        int maxTileY = (int) Math.ceil((minY + canvas.getHeight()) / tileSize);

        for (int x = minTileX; x < maxTileX; x++) {
            for (int y = minTileY; y < maxTileY; y++) {
                try {
                    var tileId = new TileId(zoom, x, y);
                    //making sure that the tile is valid, continues otherwise.
                    if (!tileId.isValid(zoom, x, y)) {
                        continue;
                    }
                    var image = tileManager.imageForTileAt(tileId);
                    graphics.drawImage(image, x * tileSize - minX, y * tileSize - minY);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Sets the {@link #redrawNeeded} flag to true and requests a pulse.
     */
    private void redrawOnNextPulse() {
        redrawNeeded = true;
        Platform.requestNextPulse();
    }
}

