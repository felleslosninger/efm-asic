package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.cms.SignerInformation;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Testing functionality.
 */
public class AsicReaderTest {

    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();

    @Test
    public void readingContentWithWriteFile() throws IOException {
        // Testing using AsicReader::writeFile.
        AsicReader asicReader = asicReaderFactory.open(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        while (asicReader.getNextFile() != null)
            asicReader.writeFile(ByteStreams.nullOutputStream());
        asicReader.close();
        Assert.assertEquals(1, asicReader.getAsicManifest().getCertificate().size());
    }

    @Test
    public void readingContentWithInputStream() throws IOException {
        // Testing using AsicReader::inputStream.
        AsicReader asicReader = asicReaderFactory.open(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        while (asicReader.getNextFile() != null)
            ByteStreams.copy(asicReader.inputStream(), ByteStreams.nullOutputStream());
        asicReader.close();
        Assert.assertEquals(1, asicReader.getAsicManifest().getCertificate().size());
    }

    @Test
    public void readingContentWithoutReading() throws IOException {
        // Testing using no functionality to read content.
        AsicReader asicReader = asicReaderFactory.open(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        while (asicReader.getNextFile() != null) {
            // No action
        }
        asicReader.close();
        Assert.assertEquals(1, asicReader.getAsicManifest().getCertificate().size());
    }

    @Test
    public void readingContentWithSigAlgCheck() throws IOException {
        // Testing using no functionality to read content.
        final AtomicBoolean wasHere = new AtomicBoolean(false);
        SignatureVerifier verifier = new SignatureVerifier() {
            @Override
            protected boolean verifySigner(SignerInformation signerInformation) {
                // Example, extend as needed
                System.out.println("Verifying signature algorithm "+signerInformation.getDigestAlgOID());
                wasHere.set(true);
                return true;
            }
        };
        AsicReader asicReader = AsicReaderFactory.newFactory(SignatureMethod.CAdES, verifier).open(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        while (asicReader.getNextFile() != null) {
            asicReader.writeFile(ByteStreams.nullOutputStream());
        }
        asicReader.close();
        Assert.assertEquals(1, asicReader.getAsicManifest().getCertificate().size());

        Assert.assertTrue(wasHere.get());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void exceptionOnEmpty() throws IOException {
        AsicReader asicReader = asicReaderFactory.open(getClass().getResourceAsStream("/asic-cades-test-valid.asice"));
        while (asicReader.getNextFile() != null)
            asicReader.writeFile(ByteStreams.nullOutputStream());

        // Trigger exception.
        asicReader.inputStream();

        Assert.fail("Exception not triggered.");
    }
}
