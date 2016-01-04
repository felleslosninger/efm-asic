package no.difi.asic;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

public class AsicVerifier extends AbstractAsicReader {

    AsicVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        super(messageDigestAlgorithm, inputStream);

        while (getNextFile() != null)
            writeFile(ByteStreams.nullOutputStream());

        close();
    }
}
