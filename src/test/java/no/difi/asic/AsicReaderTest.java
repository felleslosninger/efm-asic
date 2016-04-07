package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

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
