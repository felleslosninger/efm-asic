package no.difi.asic;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author steinar
 *         Date: 21.07.15
 *         Time: 18.48
 */
public class TestUtil {

    public static final String KEY_STORE_RESOURCE_NAME = "kontaktinfo-client-test.jks";
    public static final String BII_SAMPLE_MESSAGE_XML = "bii-trns081.xml";

    /**
     * Provides simple access to the KeyStore file provided as part of the distribution.
     * <p/>
     * The key store provides a private key and a certificate, which is used for testing purposes.
     */
    public static File keyStoreFile() {
        String pathname = "src/test/resources/kontaktinfo-client-test.jks";


        URL keyStoreResourceURL = TestUtil.class.getClassLoader().getResource(KEY_STORE_RESOURCE_NAME);
        try {
            URI uri = keyStoreResourceURL.toURI();

            File file = new File(uri);
            if (!file.canRead()) {
                throw new IllegalStateException("Unable to locate " + KEY_STORE_RESOURCE_NAME + " in class path");
            }
            return file;

        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to convert URL of keystore " + KEY_STORE_RESOURCE_NAME + " into a URI");
        }
    }

    public static String keyStorePassword() {
        return "changeit";
    }

    public static String privateKeyPassword() {
        return "changeit";
    }

    public static String keyPairAlias() {
        return "client_alias";
    }
}
