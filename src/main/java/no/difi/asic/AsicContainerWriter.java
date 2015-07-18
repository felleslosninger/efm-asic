package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds an ASiC-E container using a variation of "builder pattern".
 *
 * This class is not thread safe, as it holds a MessageDigest object.
 *
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.09
 */
public class AsicContainerWriter {

    public static final Logger log = LoggerFactory.getLogger(AsicContainerWriter.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    private static MessageDigestAlgorithm messageDigestAlgorithm = MessageDigestAlgorithm.SHA256;

    private ZipOutputStream zipOutputStream;
    private MessageDigest messageDigest;
    private AsicManifest asicManifest;
    private boolean finished = false;

    private Path containerPath = null;
    private OutputStream containerOutputStream = null;

    // Helper method
    public AsicContainerWriter(File outputDir, String filename) throws IOException {
        this(new File(outputDir, filename));
    }

    // Helper method
    public AsicContainerWriter(File file) throws IOException {
        this(file.toPath());
    }

    // Helper method
    public AsicContainerWriter(Path path) throws IOException {
        this(Files.newOutputStream(path));
        containerPath = path;
    }

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public AsicContainerWriter(OutputStream outputStream) {
        // Keep original output stream
        containerOutputStream = outputStream;

        // Initiate manifest
        asicManifest = new AsicManifest(messageDigestAlgorithm);

        // Initiate zip container
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);

        // Write mimetype file to container
        putMimeTypeAsFirstEntry();

        // Create message digester
        try {
            messageDigest = MessageDigest.getInstance(messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()));
        }
    }

    // Helper method
    public AsicContainerWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    // Helper method
    public AsicContainerWriter add(File file, String filename) throws IOException {
        return add(file.toPath(), filename);
    }

    // Helper method
    public AsicContainerWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    // Helper method
    public AsicContainerWriter add(Path path, String filename) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, filename);
        inputStream.close();
        return this;
    }

    /**
     * Add content to container. Content type is detected using filename.
     *
     * @param inputStream Content to write to container
     * @param filename Filename to use inside container
     * @return Return self to allow using builder pattern
     * @throws IOException
     */
    public AsicContainerWriter add(InputStream inputStream, String filename) throws IOException {
        // Use Files to find content type
        String mimeType = Files.probeContentType(Paths.get(filename));

        // Use URLConnection to find content type
        if (mimeType == null) {
            log.info("Unable to determine MIME type using Files.probeContentType(), trying URLConnection.getFileNameMap()");
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        }

        // Throw exception if content type is not detected
        if (mimeType == null) {
            throw new IllegalStateException(String.format("Unable to determine MIME type of %s", filename));
        }

        // Add file to container
        return add(inputStream, filename, mimeType);
    }

    // Helper method
    public AsicContainerWriter add(File file, String filename, String mimeType) throws IOException {
        return add(file.toPath(), filename, mimeType);
    }

    // Helper method
    public AsicContainerWriter add(Path path, String filename, String mimeType) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, filename, mimeType);
        inputStream.close();
        return this;
    }

    /**
     * Add content to container.
     *
     * @param inputStream Content to write to container
     * @param filename Filename to use inside container
     * @param mimeType Content type of inputStream
     * @return Return self to allow using builder pattern
     * @throws IOException
     */
    public AsicContainerWriter add(InputStream inputStream, String filename, String mimeType) throws IOException {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Creates new zip entry
        log.debug(String.format("Writing file %s to container", filename));
        zipOutputStream.putNextEntry(new ZipEntry(filename));

        // Prepare for calculation of message digest
        messageDigest.reset();
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(zipOutputStream, messageDigest);

        // Copy inputStream to zip file
        IOUtils.copy(inputStream, zipOutputStreamWithDigest);
        zipOutputStreamWithDigest.flush();

        // Close zip entry
        zipOutputStream.closeEntry();

        // Add file to manifest
        asicManifest.add(filename, mimeType, messageDigest.digest());

        return this;
    }

    /**
     * Sign and close container.
     *
     * @param keyStoreResourceName File reference for location of keystore.
     * @param keyStorePassword Password for keystore.
     * @param privateKeyPassword Password for pricate key.
     * @return Return self to allow using builder pattern
     */
    public AsicContainerWriter sign(File keyStoreResourceName, String keyStorePassword, String privateKeyPassword) {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Flip status
        finished = true;

        // Adding signature file to manifest before signing
        asicManifest.setSignature("META-INF/signature.p7s", "application/x-pkcs7-signature");

        // Generate and write manifest (META-INF/asicmanifest.xml)
        byte[] manifestBytes = asicManifest.toBytes();
        writeZipEntry(new ZipEntry("META-INF/asicmanifest.xml"), manifestBytes);

        // Generate and write signature (META-INF/signature.p7s)
        SignatureHelper signatureHelper = new SignatureHelper(keyStoreResourceName, keyStorePassword, privateKeyPassword);
        writeZipEntry(new ZipEntry("META-INF/signature.p7s"), signatureHelper.signData(manifestBytes));

        // Close container
        try {
            zipOutputStream.finish();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to finish the container: %s", e.getMessage()), e);
        }

        if (containerPath != null) {
            try {
                containerOutputStream.flush();
                containerOutputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Unable to close file: %s", e.getMessage()), e);
            }
        }

        return this;
    }

    public File getContainerFile() {
        return getContainerPath().toFile();
    }

    public Path getContainerPath() {
        if (containerPath == null)
            throw new IllegalStateException("Output path is not known");

        return containerPath;
    }

    public AsicManifest getAsicManifest() {
        return asicManifest;
    }

    /**
     * Adds the "mimetype" object to the archive
     */
    private void putMimeTypeAsFirstEntry() {
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);
        mimetypeEntry.setMethod(ZipEntry.STORED);
        mimetypeEntry.setSize(APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes().length);

        CRC32 crc32 = new CRC32();
        crc32.update(APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes());
        mimetypeEntry.setCrc(crc32.getValue());

        writeZipEntry(mimetypeEntry, APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes());
    }

    private void writeZipEntry(ZipEntry zipEntry, byte[] bytes) {
        try {
            log.debug(String.format("Writing file %s to container", zipEntry.getName()));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(bytes);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to create new ZIP entry for %s: %s", zipEntry.getName(), e.getMessage()), e);
        }
    }
}
