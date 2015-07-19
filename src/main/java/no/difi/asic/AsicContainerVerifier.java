package no.difi.asic;

import org.apache.commons.io.output.NullOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class AsicContainerVerifier extends AsicContainerReader {

    private static final Logger log = LoggerFactory.getLogger(AsicContainerVerifier.class);

    AsicContainerVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        super(messageDigestAlgorithm, inputStream);

        String filename;
        while ((filename = getNextFile()) != null) {
            log.info(String.format("Found file: %s", filename));
            writeFile(new NullOutputStream());
        }

        close();
    }
}
