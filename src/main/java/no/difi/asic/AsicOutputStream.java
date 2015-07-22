package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Stream handling requirements to ASiC files.
 */
class AsicOutputStream extends ZipOutputStream {

    public static final Logger log = LoggerFactory.getLogger(AsicOutputStream.class);

    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    public AsicOutputStream(OutputStream out) throws IOException {
        super(out);

        setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);
        putMimeTypeAsFirstEntry(APPLICATION_VND_ETSI_ASIC_E_ZIP);
    }

    private void putMimeTypeAsFirstEntry(String mimeType) throws IOException {
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setComment("mimetype=" + mimeType);
        mimetypeEntry.setMethod(ZipEntry.STORED);
        mimetypeEntry.setSize(mimeType.getBytes().length);

        CRC32 crc32 = new CRC32();
        crc32.update(mimeType.getBytes());
        mimetypeEntry.setCrc(crc32.getValue());

        writeZipEntry(mimetypeEntry, mimeType.getBytes());
    }

    protected void writeZipEntry(String filename, byte[] bytes) throws IOException {
        writeZipEntry(new ZipEntry(filename), bytes);
    }

    protected void writeZipEntry(ZipEntry zipEntry, byte[] bytes) throws IOException {
        try {
            log.debug(String.format("Writing file %s to container", zipEntry.getName()));
            putNextEntry(zipEntry);
            write(bytes);
            closeEntry();
        } catch (IOException e) {
            throw new IOException(String.format("Unable to create new ZIP entry for %s: %s", zipEntry.getName(), e.getMessage()), e);
        }
    }

}
