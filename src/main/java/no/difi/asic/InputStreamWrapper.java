package no.difi.asic;

import java.io.IOException;
import java.io.InputStream;

/**
 * Exposes the current zip entry without letting the user close the underlying zip stream.
 * <p>
 * skip() is deliberately not delegated, as DigestInputStream does not include skipped bytes in the digest.
 * The default implementation reads through read(byte[], int, int) instead.
 * <p>
 * available() is deliberately not delegated, as ZipInputStream returns 1 until end of entry. Callers using
 * available() as a chunk size (e.g. Guava ByteStreams.skipFully) would then process one byte at a time.
 */
class InputStreamWrapper extends InputStream {

    private InputStream source;

    public InputStreamWrapper(InputStream source) {
        this.source = source;
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return source.read(b, off, len);
    }
}
