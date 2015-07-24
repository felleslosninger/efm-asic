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

    // Fetched from http://begrep.difi.no/SikkerDigitalPost/1.2.0/eksempler/post.asice.zip
    @Test
    public void validSdp() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-external-sdp.asice"));
        assertEquals(asicVerifier.getAsicManifest().getFiles().size(), 6);
    }

    // Fetched from https://github.com/open-eid/digidoc4j/blob/master/testFiles/test.asice
    @Test
    public void validDigidoc4j() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-external-digidoc4j.asice"));
        assertEquals(asicVerifier.getAsicManifest().getFiles().size(), 2);
    }

    // Fetched from https://github.com/esig/dss/blob/master/dss-asic/src/test/resources/plugtest/esig2014/ESIG-ASiC/EE_AS/Signature-A-EE_AS-1.asice
    @Test
    public void validDss() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-xades-external-dss.asice"));
        assertEquals(asicVerifier.getAsicManifest().getFiles().size(), 1);
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
