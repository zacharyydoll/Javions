package ch.epfl.javions.demodulation;

import ch.epfl.javions.Preconditions;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents an object capable of computing the power of a signal from a stream of unsigned samples produced
 * by a {@link SamplesDecoder}.
 *
 * @author : Alexei Thornber (363088)
 * @author : Zachary Doll (356458)
 */
public final class PowerComputer {
    private final int BATCH_SIZE;
    private final int NB_OF_SAMPLES = 8; // Number of samples in a batch
    private final short[] last8Samples; // Temporary array that will constantly be modified.

    private final short[] decodedSamples; // Contains the decoded batch (sample of 12bits in big-endian notation)
    private final SamplesDecoder samplesDecoder;


    /**
     * Builds a new power computer that can compute the power samples of a signal, given the unsigned samples
     * produced by a {@link SamplesDecoder}.
     *
     * @param stream    the stream of samples to read from
     * @param batchSize the number of samples to read at once (must be a multiple of 8 and strictly positive)
     * @throws IllegalArgumentException if the batch size is not a multiple of 8, or inferior or equal to 0
     */
    public PowerComputer(InputStream stream, int batchSize) {
        Preconditions.checkArgument(batchSize % NB_OF_SAMPLES == 0);
        Preconditions.checkArgument(batchSize > 0);

        BATCH_SIZE = batchSize;
        samplesDecoder = new SamplesDecoder(stream, batchSize * Short.BYTES);
        decodedSamples = new short[batchSize * Short.BYTES];
        last8Samples = new short[NB_OF_SAMPLES];
    }

    /**
     * Reads the necessary number of samples from the stream and computes the power of each batch of samples.
     *
     * @param batch the array containing the batch of power samples (must be of size {@link #BATCH_SIZE})
     * @return the number of samples placed in the batch
     * @throws IllegalArgumentException if the batch size is not equal to the standard batch size
     * @throws IOException              if the stream cannot be read from (see {@link InputStream#readNBytes(byte[], int, int)})
     */
    public int readBatch(int[] batch) throws IOException {
        Preconditions.checkArgument(batch.length == BATCH_SIZE);

        int length = samplesDecoder.readBatch(decodedSamples);
        int index = 0;

        for (int i = 0; i < length / 2; i++) {
            last8Samples[(2 * i) % (Byte.SIZE)] = decodedSamples[2 * i];
            last8Samples[(2 * i + 1) % (Byte.SIZE)] = decodedSamples[(2 * i) + 1];

            var sum1 = last8Samples[6] - last8Samples[4] + last8Samples[2] - last8Samples[0];
            var sum2 = last8Samples[7] - last8Samples[5] + last8Samples[3] - last8Samples[1];
            batch[i] = sum1 * sum1 + sum2 * sum2;

            index++;
        }
        return index;
    }
}