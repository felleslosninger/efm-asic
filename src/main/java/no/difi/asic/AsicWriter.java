package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface AsicWriter {
    /**
     * Adds another data object to the ASiC archive.
     *
     * @param file references the file to be added as a data object. The name of the entry is
     *             extracted from the File object.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter add(File file) throws IOException;

    /**
     * Adds another data object to the ASiC container, using the supplied name as the zip entry name
     * @param file references the file to be added as a data object.
     * @param entryName the archive entry name to be used.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter add(File file, String entryName) throws IOException;

    /**
     * Adds another data object to the ASiC archive
     * @param path references the file to be added.
     *
     * @return reference to this AsicWriter
     * @throws IOException
     *
     * @see #add(File)
     */
    AsicWriter add(Path path) throws IOException;

    /**
     * Adds another data object to the ASiC container under the entry name provided.
     *
     * @param path reference to this AsicWriter.
     * @param entryName the archive entry name to be used.
     * @return reference to this AsicWriter
     * @throws IOException
     *
     * @see #add(File, String)
     */
    AsicWriter add(Path path, String entryName) throws IOException;

    /**
     * Adds the data provided by the stream into the ASiC archive, using the name of the supplied file as the
     * entry name.
     * @param inputStream input stream of data.
     * @param filename the name of a file, which must be available in the file system in order to determine the MIME type.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter add(InputStream inputStream, String filename) throws IOException;

    /**
     * Adds the contents of a file into the ASiC archive using the supplied entry name and MIME type.
     *
     * @param file references the file to be added as a data object.
     * @param entryName the archive entry name to be used.
     * @param mimeType explicitly identifies the MIME type of the entry.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter add(File file, String entryName, MimeType mimeType) throws IOException;

    /** @see #add(File, String, MimeType)  */
    AsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException;

    /**
     *  Adds the contents of an input stream into the ASiC archive, under a given entry name and explicitly
     *  identifying the MIME type.
     *
     * @see #add(Path, String, MimeType)
     */
    AsicWriter add(InputStream inputStream, String filename, MimeType mimeType) throws IOException;

    /**
     * Specifies which entry (file) represents the "root" document, i.e. which business document to read first.
     *
     * @param name of entry holding the root document.
     * @return reference to this AsicWriter
     */
    AsicWriter setRootEntryName(String name);

    /**
     * Signs and closes the ASiC archive. The private and public key is obtained from the supplied key store.
     *
     * @param keyStoreFile the file holding the JKS keystore file.
     * @param keyStorePassword password for the keystore
     * @param keyPassword password protecting the private key.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException;

    /**
     * Signs and closes the ASiC archive using the private and public key stored in the supplied key store under the supplied alias name.
     *
     * @param keyStoreFile the file holding the JKS keystore file.
     * @param keyStorePassword password for the keystore
     * @param keyAlias the alias of the keystore entry holding the private and the public key.
     * @param keyPassword password protecting the private key.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException;

    /**
     * Signs and closes the ASiC archive using the private and public key stored in the supplied key store under the supplied alias name.
     *
     * @param keyStoreFile
     *            the file holding the keystore file.
     * @param keyStorePassword
     *            password for the keystore
     * @param keyStoreType
     *            Type of keyStore
     * @param keyAlias
     *            the alias of the keystore entry holding the private and the public key.
     * @param keyPassword
     *            password protecting the private key.
     * @return reference to this AsicWriter
     * @throws IOException
     */
    AsicWriter sign (File keyStoreFile, String keyStorePassword, KeyStoreType keyStoreType, String keyAlias, String keyPassword) throws IOException;

    /**
     * Allows re-use of the same SignatureHelper object when creating multiple ASiC archive and hence the need to create multiple signatures.
     *
     * @param signatureHelper instantiated SignatureHelper
     * @return reference to this AsicWriter
     * @see #sign(File, String, String, String)
     * @throws IOException
     */
    AsicWriter sign(SignatureHelper signatureHelper) throws IOException;
}
