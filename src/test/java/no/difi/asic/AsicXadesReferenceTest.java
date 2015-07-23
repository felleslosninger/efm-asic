package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class AsicXadesReferenceTest {

    private static Logger log = LoggerFactory.getLogger(AsicXadesReferenceTest.class);

    private AsicVerifierFactory asicVerifierFactory = AsicVerifierFactory.newFactory(SignatureMethod.XAdES);
    private AsicReaderFactory asicRederFactory = AsicReaderFactory.newFactory(SignatureMethod.XAdES);

    @Test
    public void valid() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-valid.asice"));
        assertEquals(6, asicVerifier.getAsicManifest().getFiles().size());
    }

    @Test(enabled = false)
    public void invalidManifest() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-invalid-manifest.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test(enabled = false)
    public void invalidSignature() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-invalid-signature.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test(enabled = false)
    public void invalidSignedProperties() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-invalid-signedproperties.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }
}
