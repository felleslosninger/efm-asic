package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AsicContainerReader {

    private static final Logger log = LoggerFactory.getLogger(AsicContainerReader.class);

    protected MessageDigestAlgorithm messageDigestAlgorithm;
    protected MessageDigest messageDigest;

    protected ZipInputStream zipInputStream;
    protected ZipEntry zipEntry;

    AsicContainerReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
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

    public String getNextFile() throws IOException {
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            log.info(String.format("Found file: %s", zipEntry.getName()));

            if (!specialFile())
                return zipEntry.getName();
        }

        return null;
    }

    // Helper method
    public void writeFile(File file) throws IOException {
        writeFile(file.toPath());
    }

    // Helper method
    public void writeFile(Path path) throws IOException {
        OutputStream outputStream = Files.newOutputStream(path);
        writeFile(outputStream);
        outputStream.close();
    }

    public void writeFile(OutputStream outputStream) throws IOException {
        if (zipEntry == null)
            throw new IllegalStateException("");

        messageDigest.reset();
        DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
        IOUtils.copy(zipInputStream, digestOutputStream);

        zipInputStream.closeEntry();

        log.debug(String.format("Digest: %s", new String(Base64.encode(messageDigest.digest()))));
    }

    public void close() throws IOException {
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
            if (!AsicAbstractContainerWriter.APPLICATION_VND_ETSI_ASIC_E_ZIP.equals(baos.toString()))
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
