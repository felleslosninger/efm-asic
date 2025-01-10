package no.difi.asic.zipbomb;

import java.io.ByteArrayOutputStream;

/**
 * Use this as a simplified replacement for ByteArrayOutputStream.
 * It will abort writing to the output stream by throwing RuntimeException when configured size has been exceeded.
 */
public class MaxSizeProtectedOutputStream extends ByteArrayOutputStream {

    private final long MAX_SIZE_DECOMPRESSED;

    /**
     * Default limit is set to 1 MiB
     */
    public MaxSizeProtectedOutputStream() {
        this(1024L * 1024); // defaults to 1 MiB
    }

    /**
     * Set any limit you want as long as it's more than 1024 bytes
     */
    public MaxSizeProtectedOutputStream(final long maxSize) {
        super();
        MAX_SIZE_DECOMPRESSED = maxSize;
        if (maxSize < 1024L) throw new IllegalArgumentException("Max size should be at least 1 KiB");
    }

    @Override
    public void write(int b) {
        super.write(b);
        if (count > MAX_SIZE_DECOMPRESSED) throwSizeLimitExceeded(count);
    }

    @Override
    public void write(byte b[], int off, int len) {
        super.write(b, off, len);
        if (count > MAX_SIZE_DECOMPRESSED) throwSizeLimitExceeded(count);
    }

    private void throwSizeLimitExceeded(long size) {
        throw new RuntimeException("Output exceeds max configured size of " + MAX_SIZE_DECOMPRESSED + ", aborted after " + size + " bytes");
    }

}
