package no.difi.asic.zipbomb;

import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class MimetypeZipBombTest {

    private static final int DECOMPRESSED_SIZE = 64 * 1024 * 1024; // 64 MiB of zeros

    @Test(timeOut = 10_000)
    public void hugeMimetypeEntryIsRejectedWithoutReadingItAll() throws IOException {
        byte[] container = containerWithHugeMimetype();
        assertTrue(container.length < DECOMPRESSED_SIZE / 100, "Test container should be highly compressed");

        try (AsicReader asicReader = AsicReaderFactory.newFactory().open(new ByteArrayInputStream(container))) {
            IllegalStateException e = expectThrows(IllegalStateException.class, asicReader::getNextFile);
            assertTrue(e.getMessage().startsWith("Content is not ASiC-E container"), e.getMessage());
            // Must be rejected by the size limit, not after decompressing the whole entry.
            assertTrue(e.getMessage().contains("mimetype entry exceeds 1024 bytes"), e.getMessage());
        }
    }

    private static byte[] containerWithHugeMimetype() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("mimetype");
            entry.setMethod(ZipEntry.DEFLATED);
            zos.putNextEntry(entry);
            byte[] zeros = new byte[64 * 1024];
            for (int written = 0; written < DECOMPRESSED_SIZE; written += zeros.length)
                zos.write(zeros);
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("content.txt"));
            zos.write("hello".getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
