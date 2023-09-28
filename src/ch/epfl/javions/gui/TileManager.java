/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents the manager of the tiles. Responsible for getting the tiles from the internet and from the disk, and for
 * caching them in the cache memory and on the disk.
 */
public final class TileManager {

    /**
     * the maximum number of tiles in the cache memory
     */
    public final static int MAX_CACHE_ENTRIES = 100;
    private final String tileServer;
    private final Path path;
    private final Map<TileId, Image> cacheMemory; //the cache in memory

    /**
     * @param path       the {@link Path} to the tile cache folder
     * @param tileServer the tile server used to get the tile images from
     * @throws IOException if the tile cache folder cannot be created (see {@link Files#createDirectory})
     */
    public TileManager(Path path, String tileServer) throws IOException {
        this.path = path;

        //if the tile cache folder doesn't exist, create it a first time.
        if (!Files.exists(this.path)) {
            System.out.println("Creating directory...");
            Files.createDirectory(path);
        }
        this.tileServer = "https://" + tileServer;
        this.cacheMemory = new LinkedHashMap<>() {
            //remove the eldest entry when the cache memory is full (100)
            @Override
            protected boolean removeEldestEntry(Map.Entry<TileId, Image> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        };
    }

    /**
     * Returns the {@link Image} corresponding to the given tile's identity.
     *
     * @param tileID the tile's identity
     * @return the image corresponding to the given tile's identity
     * @throws IOException if the tile cannot be read from the internet or the disk
     */
    public Image imageForTileAt(TileId tileID) throws IOException {

        //if the tile is in the cache memory, return it
        if (cacheMemory.containsKey(tileID))
            return cacheMemory.get(tileID);

        else if (Files.exists(tileID.createPath(path)))
            return new Image(Files.newInputStream(tileID.createPath(path)));

            //if the tile is not in the cache memory or on the disk, read it from the internet
        else {
            String url = tileID.createURL(tileServer);
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", "Javions");

            try (InputStream is = connection.getInputStream()) {
                byte[] bytes = is.readAllBytes();

                Path file = tileID.createPath(path);
                if (!Files.exists(file.getParent()))
                    Files.createDirectories(file.getParent());

                Files.write(file, bytes);
                Image image = new Image(new ByteArrayInputStream(bytes));

                cacheMemory.put(tileID, image);
                return image;
            }
        }
    }

    /**
     * Represents a tile's identity, characterized by its zoom level, and its X and Y coordinates on the map.
     *
     * @param zoom : the zoom level corresponding to the tile's identity
     * @param x    : tile's X coordinate (origin is at the top left corner of the visible map)
     * @param y    : tile's Y coordinate (origin is at the top left corner of the visible map)
     */
    record TileId(int zoom, int x, int y) {
        public static final int TILE_SIZE = 256;

        /**
         * @param zoom current zoom level
         * @param x    tile's X coordinate (origin is at the top left corner of the visible map)
         * @param y    tile's Y coordinate (origin is at the top left corner of the visible map)
         * @return true if the given tile coordinates are valid, false otherwise
         */
        public boolean isValid(int zoom, int x, int y) {

            int x1 = Math.floorDiv(x, TILE_SIZE);
            int y1 = Math.floorDiv(y, TILE_SIZE);
            int maxIndex = (int) Math.pow(2, zoom) - 1;
            return x >= 0 && y >= 0 && x1 <= maxIndex && y1 <= maxIndex;
        }

        /**
         * Creates a {@link Path} to the tile's image in the tile cache folder.
         *
         * @param path first part of the path to add on to
         * @return the path to the tile's image in the cache folder, in the format "path/zoom/X/Y.png"
         */
        private Path createPath(Path path) {
            Path zoomDir = path.resolve(Integer.toString(zoom()));
            Path xDir = zoomDir.resolve(Integer.toString(x()));
            return xDir.resolve(this.y() + ".png");
        }

        /**
         * Creates the URL string to the tile's image on the internet.
         *
         * @param tileServer the tile server used to get the tile images from
         * @return the URL to the tile's image in the String format "{@link #tileServer}/zoom/x/y.png"
         */
        private String createURL(String tileServer) {
            return tileServer + "/" + zoom() + "/" + x() + "/" + y() + ".png";
        }
    }
}
