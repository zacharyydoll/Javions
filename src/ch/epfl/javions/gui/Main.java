/**
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */

package ch.epfl.javions.gui;

import ch.epfl.javions.ByteString;
import ch.epfl.javions.GeoPos;
import ch.epfl.javions.adsb.CallSign;
import ch.epfl.javions.adsb.Message;
import ch.epfl.javions.adsb.MessageParser;
import ch.epfl.javions.adsb.RawMessage;
import ch.epfl.javions.aircraft.AircraftData;
import ch.epfl.javions.aircraft.AircraftDatabase;
import ch.epfl.javions.aircraft.AircraftRegistration;
import ch.epfl.javions.aircraft.IcaoAddress;
import ch.epfl.javions.demodulation.AdsbDemodulator;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static ch.epfl.javions.Units.Time.MILLI_SECOND;
import static ch.epfl.javions.Units.Time.NANO_SECOND;
import static ch.epfl.javions.Units.convert;
import static ch.epfl.javions.Units.convertTo;
import static javafx.scene.input.KeyCode.ENTER;

/**
 * Main class of the application.
 * <p>
 *
 * @see Application for more information
 */
public final class Main extends Application {
    public static final int START_ZOOM = 8;
    public static final int START_X = 33_530;
    public static final int START_Y = 23_070;
    public static final int STAGE_MIN_WIDTH = 800;
    public static final int STAGE_MIN_HEIGHT = 600;
    public static final double TABLE_RST_HEIGHT = 0.5;
    public static final String TILE_SERVER = "tile.openstreetmap.org";
    public static final String CACHE = "tile-cache";
    private ChangeListener<GeoPos> listener = null;
    private ObservableAircraftState followedAircraft = null;

    /**
     * Main method of the application.
     *
     * @param args the command line arguments
     * @see Application#launch(String...)
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If the URL cannot be found. (see {@link URL#toURI()})
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        var dbURL = getClass().getResource("/aircraft.zip");
        assert dbURL != null;

        var fileName = Path.of(dbURL.toURI()).toString();
        var messageQueue = new ConcurrentLinkedQueue<RawMessage>();
        var messageCount = new SimpleLongProperty(0);

        Supplier<RawMessage> messageSupplier = getSupplier(getParameters().getRaw().isEmpty());

        long startTime = System.nanoTime();
        Thread thread = addMessagesToQueueThread(messageSupplier, messageQueue, startTime);
        thread.setDaemon(true);
        thread.start();

        var sap = new SimpleObjectProperty<ObservableAircraftState>();
        var db = new AircraftDatabase(fileName);
        var tm = new TileManager(Path.of(CACHE), TILE_SERVER);
        var mp = new MapParameters(START_ZOOM, START_X, START_Y);
        var bmc = new BaseMapController(tm, mp);
        var asm = new AircraftStateManager(db);
        var ac = new AircraftController(mp, asm.states(), sap);
        var atc = new AircraftTableController(asm.states(), sap);

        configureAircraftSelection(bmc, atc);
        configureAircraftTracking(bmc, atc);

        var statusLine = new StatusLineController();
        statusLine.aircraftCountProperty().bind(Bindings.size(asm.states()));
        statusLine.messageCountProperty().bind(messageCount);
        statusLine.setSliderListener(ac);

        Button collapseButton = createCollapseButton();
        TextField searchBar = statusLine.getSearchField();

        /*
        Did not add the atc pane to the border pane inorder to be able to collapse it without affecting the status
        line. The positions of the panes were modified inorder to have the status line at the bottom of the window.
        */

        var borderPane = new BorderPane();
        statusLine.pane().setCenter(collapseButton);
        borderPane.setBottom(statusLine.pane());

        var root = new SplitPane(new StackPane(bmc.pane(), ac.pane()), atc.pane(), borderPane);
        root.setOrientation(Orientation.VERTICAL);

        collapseTableOnButtonPressed(collapseButton, atc, root, statusLine);
        configureSearchBar(searchBar, statusLine, asm, bmc, atc);

        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Javions");
        primaryStage.setMinWidth(STAGE_MIN_WIDTH);
        primaryStage.setMinHeight(STAGE_MIN_HEIGHT);
        primaryStage.show();

        new AnimationTimer() {

            private long lastPurgeTime = 0;

            /**
             * {@inheritDoc}
             *
             * Reads ten messages per second from the message queue, and updates the {@link AircraftStateManager}
             * with them in real time according to their {@link Message#timeStampNs()}.
             * Also calls {@link AircraftStateManager#purge()} every second.
             */
            @Override
            public void handle(long now) {

                if (messageQueue.isEmpty()) return;
                try {
                    for (int i = 0; i < 10; i++) {
                        if (messageQueue.peek() != null) {
                            Message m = MessageParser.parse(messageQueue.poll());
                            if (m != null) {
                                asm.updateWithMessage(m);
                                //increment displayed message count by 1
                                messageCount.set(messageCount.get() + 1);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                //purge every second
                long currentTime = System.nanoTime();
                if (currentTime - lastPurgeTime >= convertTo(1, NANO_SECOND)) {
                    asm.purge();
                    lastPurgeTime = currentTime;
                }
            }
        }.start();
    }

    /**
     * Provides a Message Supplier that supplies the next message to be parsed.
     *
     * @param noFileGiven boolean indicating whether a file was given as a command line argument or not
     * @return a {@link Supplier<RawMessage>} that supplies the next {@link RawMessage} to be parsed
     * @throws IOException if the stream passed to {@link AdsbDemodulator} cannot be read
     * @see Parameters#getRaw() for details on how to determine whether a file was given as a command line argument
     */
    private Supplier<RawMessage> getSupplier(boolean noFileGiven) throws IOException {

        if (noFileGiven) {
            var demodulator = new AdsbDemodulator(System.in);
            //read from standard input
            return () -> {
                try {
                    return demodulator.nextMessage();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
        } else {
            //read from file
            try {
                DataInputStream s = new DataInputStream(
                        new BufferedInputStream(
                                new FileInputStream(getParameters().getRaw().get(0))));

                return () -> {
                    try {
                        var bytes = new byte[RawMessage.LENGTH];
                        var timeStampNs = s.readLong();
                        int bytesRead = s.readNBytes(bytes, 0, bytes.length);
                        assert bytesRead == RawMessage.LENGTH;
                        var message = new ByteString(bytes);
                        return new RawMessage(timeStampNs, message);
                    } catch (EOFException e) {
                        return null;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Creates a thread that adds messages from a Supplier to the message queue, and updates the message count.
     *
     * @param messageSupplier the message supplier that supplies the next message to be parsed
     * @param messageQueue    the message queue to which the messages are added
     * @param startTime       the start time of the application
     * @return a {@link Thread} that adds {@link RawMessage}s to the message queue and updates the message count
     * @note the thread will sleep if the message's timestamp is in the future, so that the application displays the
     * aircrafts moving in real time
     * @see #getSupplier(boolean) for information on how the messages are supplied
     * @see System#nanoTime() and {@link Thread#sleep(long)} for information on how the time is measured and how the
     * thread is paused
     */
    private Thread addMessagesToQueueThread(Supplier<RawMessage> messageSupplier,
                                            ConcurrentLinkedQueue<RawMessage> messageQueue,
                                            long startTime) {

        return new Thread(() -> {
            while (true) {
                RawMessage message = messageSupplier.get();
                if (message == null) break;

                long elapsedTime = System.nanoTime() - startTime;
                long timeDifference = message.timeStampNs() - elapsedTime;

                // If the message's timestamp is in the future, wait until the timestamp is reached.
                if (timeDifference > 0) {
                    long sleepTimeMs = (long) convert(timeDifference, NANO_SECOND, MILLI_SECOND);
                    try {
                        Thread.sleep(sleepTimeMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                messageQueue.add(message);
            }
        });
    }

    /**
     * Centers the map on the selected aircraft, and selects it in the table upon double click.
     *
     * @param bmc the {@link BaseMapController} to center on the aircraft
     * @param atc the {@link AircraftTableController} to get the selected aircraft from
     * @note selecting an aircraft in the table will center the map on it without following it, and unfollow any
     * aircraft that was followed until then. (see {@link #configureAircraftTracking})
     * @see BaseMapController#centerOn(GeoPos) for information about centering the map on the aircraft
     */
    private void configureAircraftSelection(BaseMapController bmc, AircraftTableController atc) {
        atc.setOnDoubleClick(oas -> {
            // If we're currently following an aircraft, stop following it
            if (atc.isFollowing() && followedAircraft != null) {
                followedAircraft.getPositionProperty().removeListener(listener);
            }
            atc.setIsFollowing(false);
            bmc.centerOn(oas.getPosition());
        });
    }

    /**
     * Follows the selected aircraft on the map.
     *
     * @param bmc the {@link BaseMapController} to center on the aircraft
     * @param atc the {@link AircraftTableController} to get the selected aircraft from
     * @note entering a valid aircraft information in the search bar will unfollow the aircraft and center the map on
     * the newly searched for aircraft, without following it (see {@link #findAndSelectFirstMatch}).
     * <p>
     * double clicking on an aircraft in the table will center the map on it without following it, and unfollow
     * any aircraft that was followed until then. (see {@link #configureAircraftSelection})
     */
    private void configureAircraftTracking(BaseMapController bmc, AircraftTableController atc) {

        atc.setOnFollowClicked(oas -> {

            if (atc.isFollowing() && followedAircraft != null) {
                // If we're currently following an aircraft, stop following it
                if (followedAircraft.equals(oas)) {
                    // If the clicked aircraft is the one we're currently following, stop following it
                    followedAircraft.getPositionProperty().removeListener(listener);
                    atc.setIsFollowing(false);
                } else {
                    followedAircraft.getPositionProperty().removeListener(listener);
                    atc.setIsFollowing(false);
                    followedAircraft = oas;
                    listener = (obs, oldVal, newVal) -> bmc.centerOn(oas.getPosition());
                    oas.getPositionProperty().addListener(listener);
                    atc.setIsFollowing(true);
                }

            } else if (!atc.isFollowing()) {

                atc.setIsFollowing(true); // start following the aircraft if not following any yet.
                followedAircraft = oas;
                listener = (obs, oldVal, newVal) -> bmc.centerOn(oas.getPosition());
                oas.getPositionProperty().addListener(listener);
            }
        });
    }

    /**
     * Creates a collapse button, used to collapse the {@link AircraftTableController#pane()}.
     *
     * @return the {@link Button} to collapse the table pane
     * @see #collapseTableOnButtonPressed for details on how the button collapses the table
     */
    private Button createCollapseButton() {
        Button collapseButton = new Button("collapse");
        collapseButton.setStyle("-fx-alignment: top-left");
        return collapseButton;
    }

    /**
     * Configures the {@link Button} such that when it is pressed, the {@link AircraftTableController#pane()} is
     * collapsed. The button can be pressed once more to un-collapse the pane.
     *
     * @param b          Button to set action on
     * @param atc        AircraftTableController to collapse
     * @param root       SplitPane to set divider position on
     * @param statusLine StatusLineController to get height of
     */
    private void collapseTableOnButtonPressed(Button b, AircraftTableController atc,
                                              SplitPane root, StatusLineController statusLine) {

        root.heightProperty().addListener((q, o, n) -> {
            double statusLineHeight = statusLine.pane().getHeight();
            double totalHeight = n.doubleValue();
            double dividerPosition = statusLineHeight / totalHeight;
            root.setDividerPositions(TABLE_RST_HEIGHT, 1 - dividerPosition);
        });

        b.setOnAction(e -> {
            atc.setTableCollapsed(!atc.isTableCollapsed());
            atc.pane().setVisible(!atc.isTableCollapsed());
            atc.pane().setManaged(!atc.isTableCollapsed());

            if (atc.isTableCollapsed()) {
                root.setDividerPositions(1);
                b.setText("Expand");
            } else {
                b.setText("Collapse");
                //resets table to the middle of the root pane
                root.setDividerPositions(TABLE_RST_HEIGHT);
            }
        });
    }

    /**
     * Configures the given search bar to search for an aircraft according to its {@link IcaoAddress},
     * {@link AircraftRegistration} or {@link CallSign} in the given {@link AircraftStateManager} and scrolls the
     * {@link AircraftTableController} to the given result, once the ENTER button has been pressed.
     *
     * @param searchBar  the search bar
     * @param statusLine the status line of the application
     * @param asm        the aircraft state manager
     * @param bmc        the base map controller
     * @param atc        the aircraft table controller
     * @note if no aircraft matching the input is found, the status line will display "No aircraft found".
     */
    private void configureSearchBar(TextField searchBar,
                                    StatusLineController statusLine,
                                    AircraftStateManager asm,
                                    BaseMapController bmc,
                                    AircraftTableController atc) {

        searchBar.setPromptText("Search...");
        statusLine.pane().setRight(searchBar);

        searchBar.setOnKeyReleased(e -> {

            if (ENTER.equals(e.getCode())) {
                String search = searchBar.getText().toUpperCase();

                Predicate<ObservableAircraftState> icaoFilter = state -> state.getIcaoAddress().
                        string().
                        equals(search);

                Predicate<ObservableAircraftState> registrationFilter = state -> {
                    AircraftData aircraftData = state.getAircraftData();
                    return aircraftData != null
                            && aircraftData.registration() != null
                            && aircraftData.registration().string().equals(search);
                };
                Predicate<ObservableAircraftState> callSignFilter = state -> {
                    CallSign callSign = state.getCallSign();
                    return callSign != null && callSign.string().equals(search);
                };
                //search for ICAO address, then registration, then call sign
                Stream.of(icaoFilter, registrationFilter, callSignFilter)
                        .map(filter -> findAndSelectFirstMatch(asm, atc, bmc, filter))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .orElseGet(() -> {
                            System.out.println("No aircraft found");
                            return Optional.empty();
                        });
            }
        });
    }

    /**
     * Finds the first aircraft matching the given filter from the SearchBar in the given {@link AircraftStateManager},
     * selects it in the given {@link AircraftTableController} and centers the {@link BaseMapController} on it.
     *
     * @param asm    the aircraft state manager
     * @param atc    the aircraft table controller
     * @param bmc    the base map controller
     * @param filter the filter to apply
     * @return the first aircraft matching the given filter
     * @note if the system is currently following an aircraft, it will be unfollowed if the search matches another
     * aircraft, in which case the map will be centered on the new aircraft.
     */
    private Optional<ObservableAircraftState> findAndSelectFirstMatch(AircraftStateManager asm,
                                                                      AircraftTableController atc,
                                                                      BaseMapController bmc,
                                                                      Predicate<ObservableAircraftState> filter) {

        Optional<ObservableAircraftState> oasOptional = asm.states().stream().filter(filter).findFirst();

        oasOptional.ifPresent(oas -> {
            //unfollow aircraft if it is not the one being searched for
            if (followedAircraft != null && !followedAircraft.equals(oas) && listener != null) {
                atc.setIsFollowing(false);
                followedAircraft.getPositionProperty().removeListener(listener);
            }
            int index = atc.pane().getItems().indexOf(oas);
            if (index != -1) {
                atc.pane().scrollTo(index);
                atc.pane().getSelectionModel().select(index);
                bmc.centerOn(oas.getPosition());
            }
        });
        return oasOptional;
    }
}

