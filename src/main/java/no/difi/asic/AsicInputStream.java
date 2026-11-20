package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class AsicInputStream extends ZipInputStream {

    public static final Logger logger = LoggerFactory.getLogger(AsicInputStream.class);

    /**
     * Upper bound for the decompressed size of the 'mimetype' entry. A valid value is only a few dozen bytes,
     * so this protects against a zip bomb disguised as the 'mimetype' entry.
     */
    static final int MAX_MIMETYPE_SIZE = 1024;

    public AsicInputStream(InputStream in) {
        super(in);
    }

    @Override
    public ZipEntry getNextEntry() throws IOException {
        ZipEntry zipEntry = super.getNextEntry();

        if (zipEntry != null && zipEntry.getName().equals("mimetype")) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(ByteStreams.limit(this, MAX_MIMETYPE_SIZE + 1L), baos);

            if (baos.size() > MAX_MIMETYPE_SIZE) {
                logger.warn("Content of mimetype exceeds {} bytes, aborted reading it.", MAX_MIMETYPE_SIZE);
                throw new IllegalStateException("Content is not ASiC-E container.");
            }

            logger.debug("Content of mimetype: {}", baos.toString());
            if (!AsicUtils.MIMETYPE_ASICE.equals(baos.toString()))
                throw new IllegalStateException("Content is not ASiC-E container.");

            // Fetch next
            zipEntry = super.getNextEntry();
        }

        return zipEntry;
    }
}
