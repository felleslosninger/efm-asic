package no.difi.asic;

import com.google.common.io.CountingInputStream;
import no.difi.asic.zipbomb.MaxSizeProtectedOutputStream;
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
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class AsicUtilsZipBombTest {

    private static final int DECOMPRESSED_SIZE = 64 * 1024 * 1024; // 64 MiB of zeros

    private static final long MAX_MANIFEST_SIZE = 1024 * 1024; // 1 MiB

    @DataProvider
    public Object[][] manifestEntries() {
        return new Object[][]{
                {"META-INF/asicmanifest.xml"},
                {"META-INF/manifest.xml"},
        };
    }

    @Test(dataProvider = "manifestEntries", timeOut = 10_000)
    public void hugeManifestEntryIsRejectedWithoutReadingItAll(String entryName) throws IOException {
        byte[] container = containerWithHugeEntry(entryName);
        assertTrue(container.length > 32 * 1024, "Test container should hold a sizeable compressed manifest entry");
        assertTrue(container.length < DECOMPRESSED_SIZE / 100, "Test container should be highly compressed");

        CountingInputStream input = new CountingInputStream(new ByteArrayInputStream(container));
        RuntimeException e = expectThrows(RuntimeException.class, () -> AsicUtils.combine(
                () -> new MaxSizeProtectedOutputStream(MAX_MANIFEST_SIZE), new ByteArrayOutputStream(), input));
        assertTrue(e.getMessage().startsWith("Output exceeds max configured size of " + MAX_MANIFEST_SIZE), e.getMessage());

        // Must be rejected by the size limit, not after decompressing (and consuming) the whole entry.
        assertTrue(input.getCount() < 8 * 1024, "Consumed " + input.getCount() + " of " + container.length + " bytes");
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

        ByteArrayOutputStream target = new ByteArrayOutputStream();
        AsicUtils.combine(() -> new MaxSizeProtectedOutputStream(MAX_MANIFEST_SIZE), target,
                new ByteArrayInputStream(source.toByteArray()));

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
