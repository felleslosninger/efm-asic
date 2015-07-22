package no.difi.asic;

import org.apache.commons.io.output.NullOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class AsicVerifier extends AbstractAsicReader {

    AsicVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        super(messageDigestAlgorithm, inputStream);

        while (getNextFile() != null)
            writeFile(new NullOutputStream());

        close();
    }
}
