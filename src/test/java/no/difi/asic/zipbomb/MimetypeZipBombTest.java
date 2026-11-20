package no.difi.asic.zipbomb;

import com.google.common.io.CountingInputStream;
import no.difi.asic.AsicReader;
import no.difi.asic.AsicReaderFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class MimetypeZipBombTest {

    private static final int DECOMPRESSED_SIZE = 64 * 1024 * 1024; // 64 MiB of zeros

    @Test(timeOut = 10_000)
    public void hugeMimetypeEntryIsRejectedWithoutReadingItAll() throws IOException {
        byte[] container = containerWithHugeMimetype();
        assertTrue(container.length > 32 * 1024, "Test container should hold a sizeable compressed mimetype entry");
        assertTrue(container.length < DECOMPRESSED_SIZE / 100, "Test container should be highly compressed");

        CountingInputStream input = new CountingInputStream(new ByteArrayInputStream(container));
        try (AsicReader asicReader = AsicReaderFactory.newFactory().open(input)) {
            IllegalStateException e = expectThrows(IllegalStateException.class, asicReader::getNextFile);
            assertEquals(e.getMessage(), "Content is not ASiC-E container.");
        }

        // Must be rejected by the size limit, not after decompressing (and consuming) the whole entry.
        assertTrue(input.getCount() < 8 * 1024, "Consumed " + input.getCount() + " of " + container.length + " bytes");
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
