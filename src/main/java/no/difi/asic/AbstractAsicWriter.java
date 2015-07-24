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
import java.util.zip.ZipEntry;

abstract class AbstractAsicWriter implements AsicWriter {

    public static final Logger log = LoggerFactory.getLogger(AbstractAsicWriter.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    protected AsicOutputStream asicOutputStream;
    protected AbstractAsicManifest asicManifest;

    protected boolean finished = false;
    protected OutputStream containerOutputStream = null;
    protected boolean closeStreamOnClose = false;

    protected OasisManifest oasisManifest = null;

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public AbstractAsicWriter(OutputStream outputStream, boolean closeStreamOnClose, AbstractAsicManifest asicManifest) throws IOException {
        // Keep original output stream
        this.containerOutputStream = outputStream;
        this.closeStreamOnClose = closeStreamOnClose;

        // Initiate manifest
        this.asicManifest = asicManifest;

        // Initiate zip container
        asicOutputStream = new AsicOutputStream(outputStream);

        // Add mimetype to OASIS OpenDocument manifest
        oasisManifest = new OasisManifest(MimeType.forString(APPLICATION_VND_ETSI_ASIC_E_ZIP));
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
            CadesAsicWriter.log.info("Unable to determine MIME type using Files.probeContentType(), trying URLConnection.getFileNameMap()");
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        }

        // Throw exception if content type is not detected
        if (mimeType == null) {
            throw new IllegalStateException(String.format("Unable to determine MIME type of %s", filename));
        }

        // Add file to container
        return add(inputStream, filename, MimeType.forString(mimeType));
    }

    @Override
    public AsicWriter add(File file, String entryName, MimeType mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    @Override
    public AsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException {
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
    public AsicWriter add(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        if (filename.startsWith("META-INF/"))
            throw new IllegalStateException("Adding files to META-INF is not allowed.");

        // Creates new zip entry
        log.debug(String.format("Writing file %s to container", filename));
        asicOutputStream.putNextEntry(new ZipEntry(filename));

        // Prepare for calculation of message digest
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(asicOutputStream, asicManifest.getMessageDigest());

        // Copy inputStream to zip output stream
        IOUtils.copy(inputStream, zipOutputStreamWithDigest);
        zipOutputStreamWithDigest.flush();

        // Closes the zip entry
        asicOutputStream.closeEntry();

        // Adds contents of input stream to manifest which will be signed and written once all data objects have been added
        asicManifest.add(filename, mimeType);

        // Add record of file to OASIS OpenDocument Manifest
        oasisManifest.add(filename, mimeType);

        return this;
    }

    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        return sign(keyStoreFile, keyStorePassword, null, keyPassword);
    }

    /**
     * Signs and closes ASiC container.
     *
     * @param keyStoreFile File reference for location of keystore.
     * @param keyStorePassword Password for keystore.
     * @param  keyAlias Alias for private key.
     * @param keyPassword Password for private key.
     * @return Return self to allow using builder pattern
     */
    @Override
    public AbstractAsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        return sign(new SignatureHelper(keyStoreFile, keyStorePassword, keyAlias, keyPassword));
    }

    /**
     * Signs and closes ASiC container.
     * @param signatureHelper Loaded SignatureHelper.
     * @return Return self to allow using builder pattern
     * @throws IOException
     */
    @Override
    public AbstractAsicWriter sign(SignatureHelper signatureHelper) throws IOException {
        // You may only sign once
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Flip status to ensure nobody is allowed to sign more than once.
        finished = true;

        // Delegates the actual signature creation to the signature helper
        performSign(signatureHelper);

        asicOutputStream.writeZipEntry("META-INF/manifest.xml", oasisManifest.toBytes());

        // Close container
        try {
            asicOutputStream.finish();
            asicOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Unable to finish the container: %s", e.getMessage()), e);
        }

        if (closeStreamOnClose) {
            try {
                containerOutputStream.flush();
                containerOutputStream.close();
            } catch (IOException e) {
                throw new IllegalStateException(String.format("Unable to close file: %s", e.getMessage()), e);
            }
        }

        return this;
    }

    /** Creating the signature and writing it into the archive is delegated to the actual implementation */
    abstract void performSign(SignatureHelper signatureHelper) throws IOException;

    public AbstractAsicManifest getAsicManifest() {
        return asicManifest;
    }
}
