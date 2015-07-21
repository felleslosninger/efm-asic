package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

abstract class AsicAbstractWriter implements AsicWriter {

    public static final Logger log = LoggerFactory.getLogger(AsicAbstractWriter.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    protected ZipOutputStream zipOutputStream;
    protected AsicAbstractManifest asicManifest;

    protected boolean finished = false;
    protected OutputStream containerOutputStream = null;
    protected Path containerPath = null;

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public AsicAbstractWriter(OutputStream outputStream, Path containerPath, AsicAbstractManifest asicManifest) {
        // Keep original output stream
        this.containerOutputStream = outputStream;
        this.containerPath = containerPath;

        // Initiate manifest
        this.asicManifest = asicManifest;

        // Initiate zip container
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);

        // Write mimetype file to container
        putMimeTypeAsFirstEntry(APPLICATION_VND_ETSI_ASIC_E_ZIP);
    }

    // Helper method
    @Override
    public AsicWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    // Helper method
    @Override
    public AsicWriter add(File file, String entryName) throws IOException {
        return add(file.toPath(), entryName);
    }

    // Helper method
    @Override
    public AsicWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    // Helper method
    @Override
    public AsicWriter add(Path path, String entryName) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, entryName);
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
    @Override
    public AsicWriter add(InputStream inputStream, String filename) throws IOException {
        // Use Files to find content type
        String mimeType = Files.probeContentType(Paths.get(filename));

        // Use URLConnection to find content type
        if (mimeType == null) {
            AsicCadesWriter.log.info("Unable to determine MIME type using Files.probeContentType(), trying URLConnection.getFileNameMap()");
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
    @Override
    public AsicWriter add(File file, String entryName, String mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    // Helper method
    @Override
    public AsicWriter add(Path path, String entryName, String mimeType) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, entryName, mimeType);
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
    @Override
    public AsicWriter add(InputStream inputStream, String filename, String mimeType) throws IOException {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Creates new zip entry
        log.debug(String.format("Writing file %s to container", filename));
        zipOutputStream.putNextEntry(new ZipEntry(filename));

        // Prepare for calculation of message digest
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(zipOutputStream, asicManifest.getMessageDigest());

        // Copy inputStream to zip file
        IOUtils.copy(inputStream, zipOutputStreamWithDigest);
        zipOutputStreamWithDigest.flush();

        // Close zip entry
        zipOutputStream.closeEntry();

        // Add file to manifest
        asicManifest.add(filename, mimeType);

        return this;
    }

    // Helper method
    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        return sign(keyStoreFile, keyStorePassword, null, keyPassword);
    }

    /**
     * Sign and close container.
     *
     * @param keyStoreFile File reference for location of keystore.
     * @param keyStorePassword Password for keystore.
     * @param  keyAlias Alias for private key.
     * @param keyPassword Password for private key.
     * @return Return self to allow using builder pattern
     */
    @Override
    public AsicAbstractWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        return sign(new SignatureHelper(keyStoreFile, keyStorePassword, keyAlias, keyPassword));
    }

    /**
     * Sign and close container.
     * @param signatureHelper Loaded SignatureHelper.
     * @return Return self to allow using builder pattern
     * @throws IOException
     */
    @Override
    public AsicAbstractWriter sign(SignatureHelper signatureHelper) throws IOException {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Flip status
        finished = true;

        performSign(signatureHelper);

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

    abstract void performSign(SignatureHelper signatureHelper) throws IOException;

    /**
     * Adds the "mimetype" object to the archive
     */
    private void putMimeTypeAsFirstEntry(String mimeType) {
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setComment("mimetype=" + mimeType);
        mimetypeEntry.setMethod(ZipEntry.STORED);
        mimetypeEntry.setSize(mimeType.getBytes().length);

        CRC32 crc32 = new CRC32();
        crc32.update(mimeType.getBytes());
        mimetypeEntry.setCrc(crc32.getValue());

        writeZipEntry(mimetypeEntry, mimeType.getBytes());
    }

    protected void writeZipEntry(String filename, byte[] bytes) {
        writeZipEntry(new ZipEntry(filename), bytes);
    }

    protected void writeZipEntry(ZipEntry zipEntry, byte[] bytes) {
        try {
            log.debug(String.format("Writing file %s to container", zipEntry.getName()));
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(bytes);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to create new ZIP entry for %s: %s", zipEntry.getName(), e.getMessage()), e);
        }
    }

    @Override
    public File getContainerFile() {
        return getContainerPath().toFile();
    }

    @Override
    public Path getContainerPath() {
        if (containerPath == null)
            throw new IllegalStateException("Output path is not known");

        return containerPath;
    }

    public AsicAbstractManifest getAsicManifest() {
        return asicManifest;
    }
}
