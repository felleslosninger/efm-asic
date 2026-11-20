package no.difi.asic;

import com.google.common.io.ByteStreams;
import no.difi.asic.zipbomb.MaxSizeProtectedOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class AsicVerifier extends AbstractAsicReader {

    AsicVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        this(messageDigestAlgorithm, inputStream, MaxSizeProtectedOutputStream.getDefaultMaxSize());
    }

    AsicVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream, long maxMetadataSize) throws IOException {
        super(messageDigestAlgorithm, inputStream, maxMetadataSize);

        while (getNextFile() != null)
            writeFile(ByteStreams.nullOutputStream());

        close();
    }
}
