package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

abstract class AbstractAsicWriter implements AsicWriter {

    public static final Logger logger = LoggerFactory.getLogger(AbstractAsicWriter.class);

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
    AbstractAsicWriter(OutputStream outputStream, boolean closeStreamOnClose, AbstractAsicManifest asicManifest) throws IOException {
        // Keep original output stream
        this.containerOutputStream = outputStream;
        this.closeStreamOnClose = closeStreamOnClose;

        // Initiate manifest
        this.asicManifest = asicManifest;

        // Initiate zip container
        asicOutputStream = new AsicOutputStream(outputStream);

        // Add mimetype to OASIS OpenDocument manifest
        oasisManifest = new OasisManifest(MimeType.forString(AsicUtils.MIMETYPE_ASICE));
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(File file, String entryName) throws IOException {
        return add(file.toPath(), entryName);
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(Path path, String entryName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(InputStream inputStream, String filename) throws IOException {
        // Add file to container
        return add(inputStream, filename, AsicUtils.detectMime(filename));
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(File file, String entryName, MimeType mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName, mimeType);
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter add(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        // Check status
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        if (filename.startsWith("META-INF/"))
            throw new IllegalStateException("Adding files to META-INF is not allowed.");

        // Creates new zip entry
        logger.debug("Writing file '{}' to container", filename);
        asicOutputStream.putNextEntry(new ZipEntry(filename));

        // Prepare for calculation of message digest
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(asicOutputStream, asicManifest.getMessageDigest());
        // Copy inputStream to zip output stream
        ByteStreams.copy(inputStream, zipOutputStreamWithDigest);
        zipOutputStreamWithDigest.flush();

        // Closes the zip entry
        asicOutputStream.closeEntry();

        // Adds contents of input stream to manifest which will be signed and written once all data objects have been added
        asicManifest.add(filename, mimeType);

        // Add record of file to OASIS OpenDocument Manifest
        oasisManifest.add(filename, mimeType);

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        return sign(keyStoreFile, keyStorePassword, null, keyPassword);
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        return sign(keyStoreFile, keyStorePassword, SignatureHelper.DEFAULT_KEY_STORE_TYPE, keyAlias, keyPassword);
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter sign (File keyStoreFile, String keyStorePassword, KeyStoreType keyStoreType, String keyAlias, String keyPassword) throws IOException {
        return sign(new SignatureHelper(keyStoreFile, keyStorePassword, keyStoreType, keyAlias, keyPassword));
    }

    /** {@inheritDoc} */
    @Override
    public AsicWriter sign(SignatureHelper signatureHelper) throws IOException {
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
