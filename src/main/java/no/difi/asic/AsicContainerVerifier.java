package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AsicContainerVerifier {

    private static final Logger log = LoggerFactory.getLogger(AsicContainerVerifier.class);

    private List<Path> files = new ArrayList<>();

    protected MessageDigestAlgorithm messageDigestAlgorithm = MessageDigestAlgorithm.SHA256;
    protected MessageDigest messageDigest;

    public AsicContainerVerifier(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream, Path outputFolder) throws IOException {
        this.messageDigestAlgorithm = messageDigestAlgorithm;

        try {
            messageDigest = MessageDigest.getInstance(this.messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()));
        }

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // Comment in ZIP is stored in Central Directory in the end of the file.

        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            log.info(String.format("Found file: %s", zipEntry.getName()));

            if ("mimetype".equals(zipEntry.getName())) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(zipInputStream, baos);

                log.info(String.format("Content of mimetype: %s", baos.toString()));
                if (!AsicAbstractContainerWriter.APPLICATION_VND_ETSI_ASIC_E_ZIP.equals(baos.toString()))
                    throw new IllegalStateException("Content is not ASiC-E container.");
            } else if (zipEntry.getName().startsWith("META-INF/")) {
                if (zipEntry.getName().toLowerCase().contains("asicmanifest")) {
                    // TODO Utilize manifest for CAdES
                }
                if (zipEntry.getName().toLowerCase().contains("signature.p7s")) {
                    // TODO Utilize signature for CAdES
                }
            } else {
                OutputStream outputStream = outputFolder == null ? new NullOutputStream() : Files.newOutputStream(outputFolder.resolve(zipEntry.getName()));

                messageDigest.reset();
                DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
                IOUtils.copy(zipInputStream, digestOutputStream);
                outputStream.close();

                log.debug(String.format("Digest: %s", new String(Base64.encode(messageDigest.digest()))));
            }
        }
    }

    public List<Path> getFiles() {
        return files;
    }
}
