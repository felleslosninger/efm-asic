package no.difi.asic;

import com.google.common.io.CountingInputStream;
import no.difi.asic.zipbomb.MaxSizeProtectedOutputStream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Zip bomb protection of manifests read by {@link AsicUtils#combine}. The limit is lowered with the system property
 * {@value MaxSizeProtectedOutputStream#MAX_SIZE_PROPERTY}, so the tests don't need to decompress the 1 GiB default.
 */
@Test(singleThreaded = true)
public class AsicUtilsZipBombTest {

    private static final int DECOMPRESSED_SIZE = 64 * 1024 * 1024; // 64 MiB of zeros

    private static final long MAX_MANIFEST_SIZE = 1024 * 1024; // 1 MiB

    @AfterMethod(alwaysRun = true)
    public void clearSystemProperty() {
        System.clearProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY);
    }

    @DataProvider
    public Object[][] manifestEntries() {
        return new Object[][]{
                {"META-INF/asicmanifest.xml"},
                {"META-INF/manifest.xml"},
        };
    }

    // Generous timeout, building the 64 MiB test container is the slow part on a busy machine
    @Test(dataProvider = "manifestEntries", timeOut = 30_000)
    public void hugeManifestEntryIsRejectedWithoutReadingItAll(String entryName) throws IOException {
        byte[] container = containerWithHugeEntry(entryName);
        assertTrue(container.length > 32 * 1024, "Test container should hold a sizeable compressed manifest entry");
        assertTrue(container.length < DECOMPRESSED_SIZE / 100, "Test container should be highly compressed");

        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, String.valueOf(MAX_MANIFEST_SIZE));

        CloseTrackingInputStream source = new CloseTrackingInputStream(container);
        CountingInputStream input = new CountingInputStream(source);
        CloseTrackingOutputStream target = new CloseTrackingOutputStream();
        RuntimeException e = expectThrows(RuntimeException.class, () -> AsicUtils.combine(target, input));
        assertTrue(e.getMessage().startsWith("Output exceeds max configured size of " + MAX_MANIFEST_SIZE), e.getMessage());

        // Must be rejected by the size limit, not after decompressing (and consuming) the whole entry.
        assertTrue(input.getCount() < 8 * 1024, "Consumed " + input.getCount() + " of " + container.length + " bytes");

        // The source is closed as when combining succeeds, while the target is left as is: not closed,
        // and not completed into what looks like a valid container (no end of central directory record).
        assertTrue(source.closed, "Source stream should be closed");
        assertFalse(target.closed, "Target stream should not be closed");
        assertFalse(containsEndOfCentralDirectory(target.toByteArray()), "Target should not be a completed ZIP");
    }

    private static boolean containsEndOfCentralDirectory(byte[] bytes) {
        for (int i = 0; i + 3 < bytes.length; i++)
            if (bytes[i] == 'P' && bytes[i + 1] == 'K' && bytes[i + 2] == 5 && bytes[i + 3] == 6)
                return true;
        return false;
    }

    private static class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        CloseTrackingInputStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static class CloseTrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    public void manifestWithinLimitIsCombined() throws IOException {
        String manifest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<manifest:manifest xmlns:manifest=\"urn:oasis:names:tc:opendocument:xmlns:manifest:1.0\">"
                + "<manifest:file-entry manifest:full-path=\"/\" manifest:media-type=\"" + AsicUtils.MIMETYPE_ASICE + "\"/>"
                + "<manifest:file-entry manifest:full-path=\"hello.txt\" manifest:media-type=\"text/plain\"/>"
                + "</manifest:manifest>";

        ByteArrayOutputStream source = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(source)) {
            putMimetype(zos);
            putEntry(zos, "hello.txt", "hello".getBytes(StandardCharsets.UTF_8));
            putEntry(zos, "META-INF/manifest.xml", manifest.getBytes(StandardCharsets.UTF_8));
        }

        System.setProperty(MaxSizeProtectedOutputStream.MAX_SIZE_PROPERTY, String.valueOf(MAX_MANIFEST_SIZE));

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        AsicUtils.combine(target, new ByteArrayInputStream(source.toByteArray()));

        // The OASIS manifest was read through the bounded buffer and regenerated in the target container.
        List<String> names = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(target.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null)
                names.add(entry.getName());
        }
        assertEquals(names, List.of("mimetype", "hello.txt", "META-INF/manifest.xml"));
    }

    private static byte[] containerWithHugeEntry(String entryName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            putMimetype(zos);

            ZipEntry entry = new ZipEntry(entryName);
            entry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(entry);
            byte[] zeros = new byte[64 * 1024];
            for (int written = 0; written < DECOMPRESSED_SIZE; written += zeros.length)
                zos.write(zeros);
            zos.closeEntry();

            putEntry(zos, "content.txt", "hello".getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private static void putEntry(ZipOutputStream zos, String name, byte[] content) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(content);
        zos.closeEntry();
    }

    private static void putMimetype(ZipOutputStream zos) throws IOException {
        byte[] mimetype = AsicUtils.MIMETYPE_ASICE.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(mimetype);

        ZipEntry entry = new ZipEntry("mimetype");
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(mimetype.length);
        entry.setCompressedSize(mimetype.length);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(mimetype);
        zos.closeEntry();
    }
}
