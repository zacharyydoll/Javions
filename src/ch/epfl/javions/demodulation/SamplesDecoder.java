package ch.epfl.javions.demodulation;

import ch.epfl.javions.Bits;
import ch.epfl.javions.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * This class is used to decode samples, by transforming bytes transmitted by a radio into 12 bit unsigned samples.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class SamplesDecoder {

    private final int BATCH_SIZE;
    private final int BIAS = 2048;
    private final byte[] bytes;
    private final InputStream stream;


    /**
     * Creates a new decoder that reads from the given stream.
     *
     * @param stream    the stream to read from (cannot be null)
     * @param batchSize the number of samples to read at once (must be positive)
     * @throws NullPointerException     if the stream is null
     * @throws IllegalArgumentException if the batch size is not positive
     */
    public SamplesDecoder(InputStream stream, int batchSize) {
        Objects.requireNonNull(stream, "Stream cannot be null");
        Preconditions.checkArgument(batchSize > 0);

        this.stream = stream;
        this.BATCH_SIZE = batchSize;
        this.bytes = new byte[batchSize * Short.BYTES];
    }

    /**
     * Reads the number of bytes from the stored stream corresponding to the size of a batch and converts them into
     * signed 12 bit samples.
     *
     * @param batch the array to fill with the samples (must be of size {@link #BATCH_SIZE})
     * @return the number of successfully read samples
     * @throws IllegalArgumentException if the batch length is not equal to {@link #BATCH_SIZE}
     * @throws IOException              if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public int readBatch(short[] batch) throws IOException {
        Preconditions.checkArgument(batch.length == BATCH_SIZE);

        int readBytes = stream.readNBytes(bytes, 0, bytes.length);
        int count = 0;
        //reading two bytes per sample
        for (int i = 0; i < readBytes; i += 2) {
            var firstByte = bytes[i];
            var secondByte = bytes[i + 1];

            int op = (Bits.extractUInt(secondByte, 0, 4) << Byte.SIZE) | (firstByte & 0xFF);
            op -= BIAS; //recentering around zero by subtracting BIAS

            batch[count++] = (short) op;
        }
        return readBytes / 2;
    }
}
