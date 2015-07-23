package no.difi.asic;

import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;

public class AsicCadesReferenceTest {

    private static Logger log = LoggerFactory.getLogger(AsicCadesReferenceTest.class);

    private AsicVerifierFactory asicVerifierFactory = AsicVerifierFactory.newFactory(SignatureMethod.CAdES);
    private AsicReaderFactory asicRederFactory = AsicReaderFactory.newFactory(SignatureMethod.CAdES);

    @Test
    public void valid() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-valid.asice"));
        assertEquals(2, asicVerifier.getAsicManifest().getFile().size());
    }

    @Test
    public void invalidManifest() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-invalid-manifest.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        AsicReader asicReader = asicRederFactory.open(getClass().getResourceAsStream("/asic-cades-invalid-manifest.asice"));

        try {
            asicReader.getNextFile();
            fail("Exception expected");
        } catch (IllegalStateException e) {
            // Container doesn't contain content files, so first read is expected to find manifest and thus throw exception.
            log.info(e.getMessage());
        }
    }

    @Test(enabled = false)
    public void invalidSignature() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-invalid-signature.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test(enabled = false)
    public void invalidSigReference() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-invalid-sigreference.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }
}
