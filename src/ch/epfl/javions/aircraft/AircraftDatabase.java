package ch.epfl.javions.aircraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.ZipFile;

/**
 * Represents a database of aircrafts, consisting of their ICAO address, registration, type designator, model,
 * description and wake turbulence category.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class AircraftDatabase {

    private final String fileName;
    private final int ICAO_FILE_NB_START = 4;
    private final int ICAO_FILE_NB_END = 6;
    private final int ICAO_POS = 0;
    private final int REGISTRATION_POS = ICAO_POS + 1;
    private final int TYPE_DESIGNATOR_POS = REGISTRATION_POS + 1;
    private final int MODEL_POS = TYPE_DESIGNATOR_POS + 1;
    private final int DESC_POS = MODEL_POS + 1;
    private final int WTC_POS = DESC_POS + 1;


    /**
     * Creates a new AircraftDatabase given the name of the file to pull information from.
     *
     * @param fileName the name of the wanted file (cannot be null)
     * @throws NullPointerException if the file name is null
     */
    public AircraftDatabase(String fileName) {
        Objects.requireNonNull(fileName, "File name cannot be null");
        this.fileName = fileName;
    }

    /**
     * Provides the data of an aircraft within a file, given its ICAO address.
     *
     * @param address the Icao adresse of the Aircraft
     * @return the data related to the Aircraft of the given Icao address,
     * or null if the given ICAO address does not match any Aircraft, or if the buffer is null.
     * @throws IOException if the corresponding entry cannot be found (see {@link ZipFile#getInputStream})
     * @see AircraftData for the format of the returned data.
     */
    public AircraftData get(IcaoAddress address) throws IOException {

        AircraftData output = null;
        //getting the last two digits of the ICAO address and adding ".csv" to access corresponding file entry.
        String entry = address.string().substring(ICAO_FILE_NB_START, ICAO_FILE_NB_END) + ".csv";
        entry = URLDecoder.decode(entry, StandardCharsets.UTF_8);

        try (var zip = new ZipFile(fileName);
             var stream = zip.getInputStream(zip.getEntry(entry));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             var buffer = new BufferedReader(reader)) {

            String l;
            boolean found = false;

            while (!found && (l = buffer.readLine()) != null) {
                if (l.startsWith(address.string())) {
                    //splitting the line into columns at every comma, and storing the result in an array.
                    String[] data = l.split(",", -1);

                    //format : ICAO, Registration, Designator, Model, Description, WTC
                    output = new AircraftData(new AircraftRegistration(data[REGISTRATION_POS]),
                            new AircraftTypeDesignator(data[TYPE_DESIGNATOR_POS]),
                            data[MODEL_POS],
                            new AircraftDescription(data[DESC_POS]),
                            WakeTurbulenceCategory.of(data[WTC_POS]));

                    found = true;
                }
            }
        }
        return output;
    }
}
