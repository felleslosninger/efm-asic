package no.difi.asic;

import com.google.common.io.ByteStreams;
import no.difi.asic.zipbomb.MaxSizeProtectedOutputStream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The test container has META-INF/asicmanifest.xml (838 bytes) and a signature (1889 bytes) in META-INF/.
 * <p>
 * These tests modify a JVM wide system property, hence they must not run in parallel with other tests reading it.
 */
@Test(singleThreaded = true)
public class MaxMetadataSizeTest {

    private static final String VALID_CONTAINER = "/asic-cades-test-valid.asice";
    private static final long LARGEST_METADATA_ENTRY = 1889;
    private static final String LIMIT_EXCEEDED = "Output exceeds max configured size of %d, aborted after .* bytes";
    private static final String INVALID_PROPERTY = "System property no.difi.asic.maxMetadataSize must be a number of bytes between .*";

    @AfterMethod(alwaysRun = true)
    public void clearSystemProperty() {
        System.clearProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY);
    }

    private InputStream container() {
        return getClass().getResourceAsStream(VALID_CONTAINER);
    }

    private Path containerFile() throws IOException {
        Path file = Files.createTempFile("max-metadata-size", ".asice");
        try (InputStream is = container()) {
            Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }

    private static void readAll(AsicReader asicReader) throws IOException {
        try (AsicReader reader = asicReader) {
            while (reader.getNextFile() != null)
                reader.writeFile(ByteStreams.nullOutputStream());
        }
    }

    private static void assertLimitExceeded(RuntimeException e, long limit) {
        Assert.assertTrue(e.getMessage().matches(String.format(LIMIT_EXCEEDED, limit)), e.getMessage());
    }

    // --- Defaults ---

    @Test
    public void defaultIsOneGiB() {
        Assert.assertEquals(MaxSizeProtectedOutputStream.getDefaultMaxSize(), 1024L * 1024L * 1024L);
    }

    @Test
    public void blankSystemPropertyFallsBackToDefault() {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "");
        Assert.assertEquals(MaxSizeProtectedOutputStream.getDefaultMaxSize(), MaxSizeProtectedOutputStream.DEFAULT_MAX_SIZE);
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "   ");
        Assert.assertEquals(MaxSizeProtectedOutputStream.getDefaultMaxSize(), MaxSizeProtectedOutputStream.DEFAULT_MAX_SIZE);
    }

    @Test
    public void systemPropertyIsTrimmed() {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, " 2048 ");
        Assert.assertEquals(MaxSizeProtectedOutputStream.getDefaultMaxSize(), 2048L);
    }

    // --- Factory configuration, boundaries ---

    @Test
    public void readerAcceptsEntryExactlyAtLimit() throws IOException {
        AsicReader asicReader = AsicReaderFactory.newFactory().setMaxMetadataSize(LARGEST_METADATA_ENTRY).open(container());
        readAll(asicReader);
        Assert.assertEquals(asicReader.getAsicManifest().getCertificate().size(), 1);
    }

    @Test
    public void readerRejectsEntryOneByteAboveLimit() throws IOException {
        long limit = LARGEST_METADATA_ENTRY - 1;
        try (InputStream is = container()) {
            readAll(AsicReaderFactory.newFactory().setMaxMetadataSize(limit).open(is));
            Assert.fail("Expected size limit to be enforced");
        } catch (RuntimeException e) {
            assertLimitExceeded(e, limit);
        }
    }

    @Test
    public void verifierAcceptsEntryExactlyAtLimit() throws IOException {
        try (InputStream is = container()) {
            AsicVerifier verifier = AsicVerifierFactory.newFactory().setMaxMetadataSize(LARGEST_METADATA_ENTRY).verify(is);
            Assert.assertEquals(verifier.getAsicManifest().getCertificate().size(), 1);
        }
    }

    @Test
    public void verifierRejectsEntryOneByteAboveLimit() throws IOException {
        long limit = LARGEST_METADATA_ENTRY - 1;
        try (InputStream is = container()) {
            AsicVerifierFactory.newFactory().setMaxMetadataSize(limit).verify(is);
            Assert.fail("Expected size limit to be enforced");
        } catch (RuntimeException e) {
            assertLimitExceeded(e, limit);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Max size should be at least 1 KiB")
    public void readerFactoryRejectsTooSmallLimit() {
        AsicReaderFactory.newFactory().setMaxMetadataSize(1023);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Max size should be at least 1 KiB")
    public void verifierFactoryRejectsTooSmallLimit() {
        AsicVerifierFactory.newFactory().setMaxMetadataSize(1023);
    }

    @Test
    public void factoriesAcceptLargestLimit() {
        AsicReaderFactory.newFactory().setMaxMetadataSize(MaxSizeProtectedOutputStream.MAX_MAX_SIZE);
        AsicVerifierFactory.newFactory().setMaxMetadataSize(MaxSizeProtectedOutputStream.MAX_MAX_SIZE);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Max size should be at most 2147483639 bytes")
    public void readerFactoryRejectsTooLargeLimit() {
        AsicReaderFactory.newFactory().setMaxMetadataSize(MaxSizeProtectedOutputStream.MAX_MAX_SIZE + 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Max size should be at most 2147483639 bytes")
    public void verifierFactoryRejectsTooLargeLimit() {
        AsicVerifierFactory.newFactory().setMaxMetadataSize(Long.MAX_VALUE);
    }

    // --- System property ---

    @Test
    public void systemPropertyChangesDefault() throws IOException {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "1024");
        Assert.assertEquals(MaxSizeProtectedOutputStream.getDefaultMaxSize(), 1024L);

        try (InputStream is = container()) {
            readAll(AsicReaderFactory.newFactory().open(is));
            Assert.fail("Expected size limit from system property to be enforced");
        } catch (RuntimeException e) {
            assertLimitExceeded(e, 1024);
        }

        try (InputStream is = container()) {
            AsicVerifierFactory.newFactory().verify(is);
            Assert.fail("Expected size limit from system property to be enforced");
        } catch (RuntimeException e) {
            assertLimitExceeded(e, 1024);
        }
    }

    @Test
    public void readerFactorySettingOverridesSystemProperty() throws IOException {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "1024");
        AsicReader asicReader = AsicReaderFactory.newFactory().setMaxMetadataSize(4096).open(container());
        readAll(asicReader);
        Assert.assertEquals(asicReader.getAsicManifest().getCertificate().size(), 1);
    }

    @Test
    public void verifierFactorySettingOverridesSystemProperty() throws IOException {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "1024");
        try (InputStream is = container()) {
            AsicVerifier verifier = AsicVerifierFactory.newFactory().setMaxMetadataSize(4096).verify(is);
            Assert.assertEquals(verifier.getAsicManifest().getCertificate().size(), 1);
        }
    }

    @Test
    public void factorySettingIgnoresInvalidSystemProperty() throws IOException {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "abc");
        readAll(AsicReaderFactory.newFactory().setMaxMetadataSize(4096).open(container()));
        try (InputStream is = container()) {
            AsicVerifierFactory.newFactory().setMaxMetadataSize(4096).verify(is);
        }
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = INVALID_PROPERTY + "was 'abc'")
    public void invalidSystemPropertyIsRejected() {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "abc");
        MaxSizeProtectedOutputStream.getDefaultMaxSize();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = INVALID_PROPERTY + "was '100'")
    public void tooSmallSystemPropertyIsRejected() {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "100");
        MaxSizeProtectedOutputStream.getDefaultMaxSize();
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = INVALID_PROPERTY + "was '4294967296'")
    public void tooLargeSystemPropertyIsRejected() {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "4294967296");
        MaxSizeProtectedOutputStream.getDefaultMaxSize();
    }

    @Test
    public void invalidSystemPropertyIsRejectedWhenOpening() throws IOException {
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "10M");
        try (InputStream is = container()) {
            AsicReaderFactory.newFactory().open(is);
            Assert.fail("Expected invalid system property to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("System property no.difi.asic.maxMetadataSize"), e.getMessage());
        }
        try (InputStream is = container()) {
            AsicVerifierFactory.newFactory().verify(is);
            Assert.fail("Expected invalid system property to be rejected");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().startsWith("System property no.difi.asic.maxMetadataSize"), e.getMessage());
        }
    }

    // --- File handles are not leaked when opening from a path fails ---

    /**
     * The configuration must be resolved before the file is opened: with a non-existing file, an invalid
     * configuration is reported instead of NoSuchFileException, which proves the file was never opened.
     */
    @Test
    public void fileIsNotOpenedWhenSystemPropertyIsInvalid() throws IOException {
        Path missing = Files.createTempDirectory("max-metadata-size").resolve("missing.asice");
        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, "10M");
        Assert.expectThrows(IllegalArgumentException.class, () -> AsicReaderFactory.newFactory().open(missing));
        Assert.expectThrows(IllegalArgumentException.class, () -> AsicVerifierFactory.newFactory().verify(missing));
        Assert.expectThrows(IllegalArgumentException.class, () -> AsicReaderFactory.newFactory().open(missing.toFile()));
        Assert.expectThrows(IllegalArgumentException.class, () -> AsicVerifierFactory.newFactory().verify(missing.toFile()));
        Files.delete(missing.getParent());
    }

    @Test
    public void streamOpenedByFactoryIsClosedWhenVerificationFails() throws IOException {
        CloseTrackingInputStream is = new CloseTrackingInputStream(container());
        RuntimeException e = Assert.expectThrows(RuntimeException.class,
                () -> AsicVerifierFactory.newFactory().verifyClosingOnFailure(is, 1024));
        assertLimitExceeded(e, 1024);
        Assert.assertTrue(is.closed, "Stream should be closed when verification fails");
    }

    @Test
    public void verifyFromPathStillWorks() throws IOException {
        Path file = containerFile();
        try {
            AsicVerifier verifier = AsicVerifierFactory.newFactory().setMaxMetadataSize(LARGEST_METADATA_ENTRY).verify(file);
            Assert.assertEquals(verifier.getAsicManifest().getCertificate().size(), 1);
            readAll(AsicReaderFactory.newFactory().setMaxMetadataSize(LARGEST_METADATA_ENTRY).open(file));
        } finally {
            Files.delete(file);
        }
    }

    private static class CloseTrackingInputStream extends FilterInputStream {
        private boolean closed;

        CloseTrackingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
