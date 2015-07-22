package no.difi.asic;

import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ManifestVerifierTest {

    private static Logger log = LoggerFactory.getLogger(ManifestVerifierTest.class);

    @Test
    public void validateMessageDigestAlgorithm() {
        ManifestVerifier manifestVerifier = new ManifestVerifier(MessageDigestAlgorithm.SHA256);

        // Not to fail
        manifestVerifier.update("sha256", null, null, MessageDigestAlgorithm.SHA256.getUri());

        try {
            // Should fail
            manifestVerifier.update("sha384", null, null, MessageDigestAlgorithm.SHA384.getUri());
            fail("Exception expected");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        try {
            // Should fail
            manifestVerifier.update("sha512", null, null, MessageDigestAlgorithm.SHA512.getUri());
            fail("Exception expected");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test
    public void testValidDigest() {
        ManifestVerifier manifestVerifier = new ManifestVerifier(MessageDigestAlgorithm.SHA256);
        manifestVerifier.update("file", new byte[]{'c', 'a', 'f', 'e'});
        manifestVerifier.update("file", "text/plain", new byte[]{'c', 'a', 'f', 'e'}, null);
    }

    @Test
    public void testInvalidDigest() {
        ManifestVerifier manifestVerifier = new ManifestVerifier(MessageDigestAlgorithm.SHA256);
        manifestVerifier.update("file", new byte[]{'c', 'a', 'f', 'e'});

        try {
            manifestVerifier.update("file", null, new byte[]{'c', 'a', 'f', 'f'}, null);
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }
}
