package ch.epfl.javions.demodulation;

import ch.epfl.javions.adsb.RawMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Demodulates ADS-B messages from a stream of power samples.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class AdsbDemodulator {
    private static final int MESSAGE_LENGTH = 112;
    private final int SAMPLES_IN_PREAMBLE = 80;
    private final PowerWindow window;
    //The distance between two peaks
    private final int PULSE_SIZE = 5;
    private int currentPeak = 0;
    private int nextPeak;

    /**
     * Builds a new demodulator from the given stream of samples.
     *
     * @param samplesStream the stream of samples to read from
     * @throws IOException if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public AdsbDemodulator(InputStream samplesStream) throws IOException {
        window = new PowerWindow(samplesStream, 1200);
    }

    /**
     * Returns the next ADS-B message in the stream provided to the constructor.
     *
     * @return the next ADS-B message in the stream, or null if there are no more messages to read
     * @throws IOException if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public RawMessage nextMessage() throws IOException {
        while (window.isFull()) {
            //Setting the peaks correctly
            int previousPeak = currentPeak;
            currentPeak = nextPeak;
            nextPeak = window.get(1) + window.get(11) + window.get(36) + window.get(46);

            int valley = window.get(PULSE_SIZE) + window.get(3 * PULSE_SIZE) + window.get(4 * PULSE_SIZE) +
                    window.get(5 * PULSE_SIZE) + window.get(6 * PULSE_SIZE) + window.get(8 * PULSE_SIZE);

            if (messageIsStarting(previousPeak, valley)) {

                byte[] message = new byte[RawMessage.LENGTH];
                buildMessage(message, 0, Byte.SIZE);

                if (RawMessage.size(message[0]) == RawMessage.LENGTH) {
                    //Decoding the rest of the message
                    buildMessage(message, Byte.SIZE, MESSAGE_LENGTH);
                    RawMessage maybeValid = RawMessage.of(window.position() * 100, message);

                    if (maybeValid != null) {
                        window.advanceBy(window.size());
                        return maybeValid;
                    }
                }
            }
            window.advance();
        }
        return null;
    }

    /**
     * Builds the first 8 bits of the ADS-B message, representing the preamble.
     *
     * @param message the message to decode
     */
    private void buildMessage(byte[] message, int start, int end) {
        for (int i = start; i < end; i++) {
            if ((window.get(SAMPLES_IN_PREAMBLE + (10 * i))) < (window.get((SAMPLES_IN_PREAMBLE + PULSE_SIZE) + (10 * i)))) {
                message[i / Byte.SIZE] = (byte) (message[i / Byte.SIZE] << 1);
            } else {
                message[i / Byte.SIZE] = (byte) ((message[i / Byte.SIZE] << 1) | 1);
            }
        }
    }

    /**
     * Checks if the current peak is the start of an ADS-B message.
     *
     * @param previousPeak the previous peak
     * @param valley       the valley between the peaks
     * @return true if an ADS-B message is starting, false otherwise
     */
    private boolean messageIsStarting(int previousPeak, int valley) {
        return (currentPeak >= 2 * valley) && (previousPeak < currentPeak) && (currentPeak > nextPeak);
    }
}

