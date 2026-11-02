package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
    public void readingLargeContentWithInputStream() throws IOException {
        byte[] content = largeContent();

        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(signedContainer(content)));
        Assert.assertEquals(asicReader.getNextFile(), "large.bin");

        ByteArrayOutputStream result = new ByteArrayOutputStream(content.length);
        ByteStreams.copy(asicReader.inputStream(), result);
        Assert.assertEquals(result.toByteArray(), content);

        // Verifies digest of content against the signed manifest.
        Assert.assertNull(asicReader.getNextFile());
        asicReader.close();
        Assert.assertTrue(asicReader.getAsicManifest().getFile().get(0).isVerified());
    }

    @Test
    public void skippingContentWithInputStreamKeepsDigest() throws IOException {
        byte[] content = largeContent();

        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(signedContainer(content)));
        Assert.assertEquals(asicReader.getNextFile(), "large.bin");

        InputStream inputStream = asicReader.inputStream();
        ByteStreams.skipFully(inputStream, content.length);
        Assert.assertEquals(inputStream.read(), -1);

        // Skipped bytes must be part of the digest for the manifest to verify.
        Assert.assertNull(asicReader.getNextFile());
        asicReader.close();
        Assert.assertTrue(asicReader.getAsicManifest().getFile().get(0).isVerified());
    }

    private static byte[] largeContent() {
        byte[] content = new byte[32 * 1024 * 1024];
        for (int i = 0; i < content.length; i++)
            content[i] = (byte) (i % 251);
        return content;
    }

    private static byte[] signedContainer(byte[] content) throws IOException {
        ByteArrayOutputStream container = new ByteArrayOutputStream();
        AsicWriterFactory.newFactory()
                .newContainer(container)
                .add(new ByteArrayInputStream(content), "large.bin")
                .sign(TestUtil.keyStoreFile(), TestUtil.keyStorePassword(), TestUtil.privateKeyPassword());
        return container.toByteArray();
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
