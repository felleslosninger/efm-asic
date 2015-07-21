package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class AsicAbstractContainerReader {

    private static final Logger log = LoggerFactory.getLogger(AsicAbstractContainerReader.class);

    protected MessageDigestAlgorithm messageDigestAlgorithm;
    protected MessageDigest messageDigest;

    protected ZipInputStream zipInputStream;
    protected ZipEntry zipEntry;

    AsicAbstractContainerReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        this.messageDigestAlgorithm = messageDigestAlgorithm;

        try {
            messageDigest = MessageDigest.getInstance(this.messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()));
        }

        zipInputStream = new ZipInputStream(inputStream);
        // Comment in ZIP is stored in Central Directory in the end of the file.
    }

    protected String getNextFile() throws IOException {
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            log.info(String.format("Found file: %s", zipEntry.getName()));

            if (!specialFile())
                return zipEntry.getName();
        }

        return null;
    }

    protected void writeFile(OutputStream outputStream) throws IOException {
        if (zipEntry == null)
            throw new IllegalStateException("No file to read.");

        messageDigest.reset();
        DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
        IOUtils.copy(zipInputStream, digestOutputStream);

        zipInputStream.closeEntry();

        log.debug(String.format("Digest: %s", new String(Base64.encode(messageDigest.digest()))));
    }

    protected void close() throws IOException {
        if (zipInputStream != null) {
            zipInputStream.close();
            zipInputStream = null;
        }
    }

    protected boolean specialFile() throws IOException {
        if ("mimetype".equals(zipEntry.getName())) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(zipInputStream, baos);

            log.info(String.format("Content of mimetype: %s", baos.toString()));
            if (!AsicAbstractWriter.APPLICATION_VND_ETSI_ASIC_E_ZIP.equals(baos.toString()))
                throw new IllegalStateException("Content is not ASiC-E container.");
            return true;
        // } else if (zipEntry.getName().toLowerCase().contains("asicmanifest")) {
            // TODO Utilize manifest for CAdES
        // } else if (zipEntry.getName().toLowerCase().contains("signature.p7s")) {
            // TODO Utilize signature for CAdES
        }

        return false;
    }

}
