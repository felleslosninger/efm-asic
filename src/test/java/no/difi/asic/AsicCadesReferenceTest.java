package no.difi.asic;

import no.difi.commons.asic.jaxb.asic.AsicManifest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Security;

import static org.testng.Assert.*;

public class AsicCadesReferenceTest {

    private static Logger log = LoggerFactory.getLogger(AsicCadesReferenceTest.class);

    private AsicVerifierFactory asicVerifierFactory = AsicVerifierFactory.newFactory(SignatureMethod.CAdES);
    private AsicReaderFactory asicRederFactory = AsicReaderFactory.newFactory(SignatureMethod.CAdES);

    @BeforeClass
    public void beforeClass() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void valid() throws IOException {
        AsicVerifier asicVerifier = asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        assertEquals(asicVerifier.getAsicManifest().getFile().size(), 2);

        // Printing internal manifest for reference.
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AsicManifest.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            marshaller.marshal(asicVerifier.getAsicManifest(), byteArrayOutputStream);

            log.info(byteArrayOutputStream.toString());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }

    @Test
    public void invalidManifest() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-test-invalid-manifest.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        AsicReader asicReader = asicRederFactory.open(getClass().getResourceAsStream("/asic-cades-test-invalid-manifest.asice"));

        try {
            asicReader.getNextFile();
            fail("Exception expected");
        } catch (IllegalStateException e) {
            // Container doesn't contain content files, so first read is expected to find manifest and thus throw exception.
            log.info(e.getMessage());
        }
    }

    @Test
    public void invalidSignature() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-test-invalid-signature.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test
    public void invalidMetadataFile() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-test-invalid-metadata-file.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
            assertTrue(e.getMessage().contains("signature.malformed"));
        }
    }

    @Test//(enabled = false)
    public void invalidSigReference() throws IOException {
        try {
            asicVerifierFactory.verify(getClass().getResourceAsStream("/asic-cades-test-invalid-sigreference.asice"));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }
}
