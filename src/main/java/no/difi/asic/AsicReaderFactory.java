package no.difi.asic;

import no.difi.asic.zipbomb.MaxSizeProtectedOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates AsicReader instances.
 * <p>
 * Configure the factory (e.g. {@link #setMaxMetadataSize(long)}) before sharing it between threads.
 */
public class AsicReaderFactory {

    public static AsicReaderFactory newFactory() {
        return newFactory(MessageDigestAlgorithm.SHA256);
    }

    public static AsicReaderFactory newFactory(SignatureMethod signatureMethod) {
        return newFactory(signatureMethod.getMessageDigestAlgorithm());
    }

    static AsicReaderFactory newFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        return new AsicReaderFactory(messageDigestAlgorithm);
    }

    private MessageDigestAlgorithm messageDigestAlgorithm;

    /**
     * Null means default, resolved each time a container is opened.
     */
    private volatile Long maxMetadataSize;

    private AsicReaderFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    /**
     * Sets the max allowed decompressed size (in bytes) of each single file in META-INF/, e.g. the ASiC manifest
     * and signatures. This is a protection against zip bombs.
     * <p>
     * The limit applies to each META-INF/ entry separately and is not a bound on total memory use; reading an entry
     * close to the limit may temporarily use several times the limit in heap.
     * <p>
     * If not set, the value of the system property {@value MaxSizeProtectedOutputStream#MAX_SIZE_PROPERTY}
     * is used, falling back to 1 GiB. A value set here takes precedence over the system property.
     *
     * @param maxMetadataSize max size in bytes, from 1 KiB up to {@link MaxSizeProtectedOutputStream#MAX_MAX_SIZE}
     * @return this factory
     * @throws IllegalArgumentException if max size is out of bounds
     */
    public AsicReaderFactory setMaxMetadataSize(long maxMetadataSize) {
        MaxSizeProtectedOutputStream.validateMaxSize(maxMetadataSize);
        this.maxMetadataSize = maxMetadataSize;
        return this;
    }

    public AsicReader open(File file) throws IOException {
        return open(file.toPath());
    }

    public AsicReader open(Path file) throws IOException {
        // Resolve configuration before opening the file, so an invalid configuration doesn't leak a file handle.
        long resolvedMaxMetadataSize = resolveMaxMetadataSize();
        return openClosingOnFailure(Files.newInputStream(file), resolvedMaxMetadataSize);
    }

    /**
     * Used when this factory opened the stream itself, so the stream is not leaked if creating the reader fails.
     */
    AsicReader openClosingOnFailure(InputStream inputStream, long maxMetadataSize) throws IOException {
        try {
            return new AsicReaderImpl(messageDigestAlgorithm, inputStream, maxMetadataSize);
        } catch (IOException | RuntimeException e) {
            closeQuietly(inputStream, e);
            throw e;
        }
    }

    public AsicReader open(InputStream inputStream) throws IOException {
        return new AsicReaderImpl(messageDigestAlgorithm, inputStream, resolveMaxMetadataSize());
    }

    private long resolveMaxMetadataSize() {
        Long configured = maxMetadataSize;
        return configured != null ? configured : MaxSizeProtectedOutputStream.getDefaultMaxSize();
    }

    static void closeQuietly(InputStream inputStream, Exception cause) {
        try {
            inputStream.close();
        } catch (IOException suppressed) {
            cause.addSuppressed(suppressed);
        }
    }
}
