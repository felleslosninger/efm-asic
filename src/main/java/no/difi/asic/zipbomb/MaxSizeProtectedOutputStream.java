package no.difi.asic.zipbomb;

import java.io.ByteArrayOutputStream;

/**
 * Use this as a simplified replacement for ByteArrayOutputStream.
 * It will abort writing to the output stream by throwing RuntimeException when configured size has been exceeded.
 */
public class MaxSizeProtectedOutputStream extends ByteArrayOutputStream {

    /**
     * Name of the system property that can be used to override the default max size (in bytes),
     * e.g. {@code -Dno.difi.asic.maxMetadataSize=10485760} for 10 MiB.
     */
    public static final String MAX_SIZE_PROPERTY = "no.difi.asic.maxMetadataSize";

    /**
     * Built-in default max size, 1 GiB. Used when the system property {@link #MAX_SIZE_PROPERTY} is not set.
     */
    public static final long DEFAULT_MAX_SIZE = 1024L * 1024L * 1024L;

    /**
     * Smallest allowed configured max size, 1 KiB.
     */
    public static final long MIN_MAX_SIZE = 1024L;

    /**
     * Largest allowed configured max size. A {@link ByteArrayOutputStream} cannot hold more than this,
     * so a higher limit would never be reached and the JVM would run out of memory instead.
     */
    public static final long MAX_MAX_SIZE = Integer.MAX_VALUE - 8;

    private final long MAX_SIZE_DECOMPRESSED;

    /**
     * Uses the default limit, see {@link #getDefaultMaxSize()}.
     */
    public MaxSizeProtectedOutputStream() {
        this(getDefaultMaxSize());
    }

    /**
     * Set any limit you want as long as it's at least 1024 bytes
     */
    public MaxSizeProtectedOutputStream(final long maxSize) {
        super();
        MAX_SIZE_DECOMPRESSED = maxSize;
        if (maxSize < MIN_MAX_SIZE) throw new IllegalArgumentException("Max size should be at least 1 KiB");
    }

    /**
     * Returns the default max size in bytes. This is the value of the system property {@link #MAX_SIZE_PROPERTY}
     * if set, otherwise {@link #DEFAULT_MAX_SIZE} (1 GiB).
     * <p>
     * The system property is read on every call, i.e. every time a container is opened.
     *
     * @return default max size in bytes
     * @throws IllegalArgumentException if the system property is set to an invalid value
     */
    public static long getDefaultMaxSize() {
        String value = System.getProperty(MAX_SIZE_PROPERTY);
        if (value == null || value.trim().isEmpty())
            return DEFAULT_MAX_SIZE;

        try {
            long maxSize = Long.parseLong(value.trim());
            validateMaxSize(maxSize);
            return maxSize;
        } catch (IllegalArgumentException e) { // NumberFormatException is an IllegalArgumentException
            throw new IllegalArgumentException(String.format("System property %s must be a number of bytes between %d and %d, was '%s'",
                    MAX_SIZE_PROPERTY, MIN_MAX_SIZE, MAX_MAX_SIZE, value), e);
        }
    }

    /**
     * Verifies that a configured max size is within allowed bounds, {@link #MIN_MAX_SIZE} to {@link #MAX_MAX_SIZE}.
     *
     * @param maxSize max size in bytes
     * @throws IllegalArgumentException if max size is out of bounds
     */
    public static void validateMaxSize(long maxSize) {
        if (maxSize < MIN_MAX_SIZE) throw new IllegalArgumentException("Max size should be at least 1 KiB");
        if (maxSize > MAX_MAX_SIZE) throw new IllegalArgumentException("Max size should be at most " + MAX_MAX_SIZE + " bytes");
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
