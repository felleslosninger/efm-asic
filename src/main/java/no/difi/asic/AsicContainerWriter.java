package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface AsicContainerWriter {
    /**
     * Adds another data object to the ASiC archive.
     *
     * @param file references the file to be added as a data object. The name of the entry is
     *             extracted from the File object.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter add(File file) throws IOException;

    /**
     * Adds another data object to the ASiC container, using the supplied name as the zip entry name
     * @param file references the file to be added as a data object.
     * @param entryName the archive entry name to be used.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter add(File file, String entryName) throws IOException;

    /**
     * Adds another data object to the ASiC archive
     * @param path references the file to be added.
     *
     * @return reference to this AsicContainerWriter
     * @throws IOException
     *
     * @see #add(File)
     */
    AsicContainerWriter add(Path path) throws IOException;

    /**
     * Adds another data object to the ASiC container under the entry name provided.
     *
     * @param path reference to this AsicContainerWriter.
     * @param entryName the archive entry name to be used.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     *
     * @see #add(File, String)
     */
    AsicContainerWriter add(Path path, String entryName) throws IOException;

    /**
     * Adds the data provided by the stream into the ASiC archive, using the name of the supplied file as the
     * entry name.
     * @param inputStream input stream of data.
     * @param filename the name of a file, which must be available in the file system in order to determine the MIME type.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter add(InputStream inputStream, String filename) throws IOException;

    /**
     * Adds the contents of a file into the ASiC archive using the supplied entry name and MIME type.
     *
     * @param file references the file to be added as a data object.
     * @param entryName the archive entry name to be used.
     * @param mimeType explicitly identifies the MIME type of the entry.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter add(File file, String entryName, String mimeType) throws IOException;

    /** @see #add(File, String, String)  */
    AsicContainerWriter add(Path path, String entryName, String mimeType) throws IOException;

    /**
     *  Adds the contents of an input stream into the ASiC archive, under a given entry name and explicitly
     *  identifying the MIME type.
     *
     * @see #add(Path, String, String)
     */
    AsicContainerWriter add(InputStream inputStream, String filename, String mimeType) throws IOException;

    /**
     * Signs and closes the ASiC archive. The private and public key is obtained from the supplied key store.
     *
     * @param keyStoreFile the file holding the JKS keystore file.
     * @param keyStorePassword password for the keystore
     * @param keyPassword password protecting the private key.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException;

    /**
     * Signs and closes the ASiC archive using the private and public key stored in the supplied key store under the supplied alias name.
     *
     * @param keyStoreFile the file holding the JKS keystore file.
     * @param keyStorePassword password for the keystore
     * @param keyAlias the alias of the keystore entry holding the private and the public key.
     * @param keyPassword password protecting the private key.
     * @return reference to this AsicContainerWriter
     * @throws IOException
     */
    AsicContainerWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException;

    AsicContainerWriter sign(SignatureHelper signatureHelper) throws IOException;

    File getContainerFile();

    Path getContainerPath();
}
