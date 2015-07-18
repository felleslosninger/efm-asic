package no.difi.asic;

import com.sun.xml.bind.api.JAXBRIContext;
import org.apache.commons.io.IOUtils;
import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.FileNameMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds an ASiC-E container using the "builder pattern".
 *
 * This class is not thread safe, as it holds a JAXBContext and a MessageDigest object.
 *
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.09
 */
public class AsicContainerWriter {

    public static final Logger log = LoggerFactory.getLogger(AsicContainerWriter.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    public static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";

    private ZipOutputStream zipOutputStream;
    private MessageDigest messageDigest;
    private AsicManifest asicManifest = new AsicManifest();
    private boolean finished = false;

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
    }

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public AsicContainerWriter(OutputStream outputStream) {
        // Initiate zip container
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);

        // Write mimetype file to container
        putMimeTypeAsFirstEntry();

        // Create message digester
        createMessageDigester();
    }

    // Helper method
    public AsicContainerWriter add(File file) throws IOException {
        return add(file, file.getName());
    }

    // Helper method
    public AsicContainerWriter add(File file, String filename) throws IOException {
        return add(file.toPath(), filename);
    }

    // Helper method
    public AsicContainerWriter add(File file, String filename, String mimeType) throws IOException {
        return add(file.toPath(), filename, mimeType);
    }

    // Helper method
    public AsicContainerWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    // Helper method
    public AsicContainerWriter add(Path path, String filename) throws IOException {
        return add(Files.newInputStream(path), filename);
    }

    // Helper method
    public AsicContainerWriter add(Path path, String filename, String mimeType) throws IOException {
        return add(Files.newInputStream(path), filename, mimeType);
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
        if (finished)
            throw new IllegalStateException("Adding content to container after signing container is not supported.");

        // Creates new zip entry
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
        finished = true;

        byte[] manifestBytes = asicManifest.toBytes();
        writeAsicManifest(manifestBytes);

        SignatureHelper signatureHelper = new SignatureHelper(keyStoreResourceName, keyStorePassword, privateKeyPassword);
        writeSignature(signatureHelper.signData(manifestBytes));

        try {
            zipOutputStream.finish();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to finish the container. " + e.getMessage(), e);
        }

        return this;
    }

    public AsicManifest getAsicManifest() {
        return asicManifest;
    }

    // Holds the list of entries to be added to the container
    private Map<String, AsicDataObjectEntry> files = new HashMap<>();

    /**
     * Holds the archiveName of container, which may not contain special characters like for instance ':', '/', '\\'
     */
    private String archiveName;

    /** Directory into which the ASiC container should be written */
    private File outputDir;

    private JAXBContext jaxbContext;

    private File keyStoreFile;
    private String keyStorePassword;
    private String privateKeyPassword;

    public AsicContainerWriter() {

        try {
            // Creating the JAXBContext is heavy lifting, so do it only once.
            jaxbContext = JAXBRIContext.newInstance(ASiCManifestType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXBContext " +e.getMessage(), e);
        }
    }


    /**
     * Adds the supplied file to the list of files to be added into the archive,
     * using the complete path unless it is absolute in which
     * case it is added to the archive under the basic file archiveName (entire path removed).
     *
     * @param file reference to file to be added.
     *
     * @see #addFile(File, String)
     */
    public AsicContainerWriter addFile(File file) {
        if (file.isAbsolute()) {
            // Absolute file name, so use the file name as the entry name
            addFile(file, file.getName());
        } else {
            // Relative or only a file name, use it verbatim
            addFile(file, file.toString());
        }

        return this;
    }


    /**
     * Adds another file with the given name to the set of data objects to be contained in the ASiC container.
     * The MIME type and URI is computed as part of the addition process.
     */
    public AsicContainerWriter addFile(File fileReference, String entryName) {

        String mimeTypeStr = null;
        MimeType mimeType;
        URI uri;
        try {
            // See if we can figure out the MIME type
            mimeTypeStr = Files.probeContentType(fileReference.toPath());

            // Nope, let's try a different approach..
            if (mimeTypeStr == null) {

                log.info("Unable to determine MIME type using Files.probeContentType(), trying URLConnection.getFileNameMap()");
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                String contentTypeFor = fileNameMap.getContentTypeFor(fileReference.toString());

                if (contentTypeFor == null) {
                    throw new IllegalStateException("Unable to determine MIME type of " + fileReference.toPath());
                } else
                    mimeType = new MimeType(contentTypeFor);
            } else {
                mimeType = new MimeType(mimeTypeStr);
            }
            uri = new URI(entryName);

        } catch (IOException e) {
            throw new IllegalStateException("Unable to determine MIME type for " + entryName + ", file:" + fileReference,e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to create URI for " + entryName + ", file:" + fileReference,e);
        } catch (MimeTypeParseException e) {
            throw new IllegalStateException("Unable to parse MIME type " + mimeTypeStr + " for entry " + entryName + ", file:" + fileReference, e);
        }

        AsicDataObjectEntry entry = new AsicDataObjectEntry(entryName, fileReference, mimeType, uri);

        files.put(entryName, entry);
        return this;
    }


    public AsicContainerWriter keyStore(File keyStoreResourceName) {
        this.keyStoreFile = keyStoreResourceName;
        return this;
    }

    public AsicContainerWriter keyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public AsicContainerWriter privateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
        return this;
    }

    /**
     * Provides a map with the details about each data object that has been added to the container.
     */
    public Map<String, AsicDataObjectEntry> getFiles() {
        return files;
    }

    /**
     * Property getter for the ASiC archive file name.
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * Property getter for directory into which the ASiC container will be written.
     */
    public File getOutputDir() {
        return outputDir;
    }


    /**
     * Identifies the output directory into which the archive will be created.
     *
     */
    public AsicContainerWriter outputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /** The file name of the ASiC container.
     * If an extension is given like for instance <code>.zip</code> it will override
     * the default extension of either <code>.asice</code> or <code>.sce</code>
     *
     */
    public AsicContainerWriter archiveName(String name) {
        // TODO: check the extension of the name and add .asic or .sce if no extension has been provided.
        this.archiveName = name;

        // TODO: verify there are no special characters in the archiveName

        return this;
    }

    /**
     * Builds the ASiC container, i.e. creates the ZIP file.
     */
    public AsicContainer build() {

        if (getOutputDir() == null) {
            throw new IllegalStateException("Output directory for the ASiC container not specified");
        }
        if (getArchiveName() == null) {
            throw new IllegalStateException("Name of archive not specified");
        }

        File outputFile = new File(getOutputDir(), getArchiveName());
        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(outputFile);
            build(fileOutputStream);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to create output file " + outputFile, e);
        } finally {
             if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    throw new IllegalStateException("Error occured while closing file  " + outputFile, e);
                }
             }
        }

        return new AsicContainer(outputFile);
    }

    /**
     * Builds the ASiC container to outputStream.
     */
    public void build(OutputStream outputStream) {

        if (keyStoreFile == null) {
            throw new IllegalStateException("KeyStore file not specified");
        }
        if (keyStorePassword == null) {
            throw new IllegalStateException("Password for the keystore must be supplied");
        }
        if (privateKeyPassword == null) {
            throw new IllegalStateException("Password for private key within keystore must be supplied");
        }

        // Creates the messages digester for each file added to the container.
        messageDigest = createMessageDigester();

        zipOutputStream = createZipOutputStream(outputStream);

        putMimeTypeAsFirstEntry();

        // Adds all the files, computing the digest values for each one as we iterate over them
        transferFilesIntoArchive(getFiles(), messageDigest);

        // Creates the ASicManiFest and shoves it into the ZipFile
        AsicManifestReference asicManifestReference = new AsicManifestReference(getFiles().values());
        byte[] manifestBytes = asicManifestReference.toBytes(jaxbContext);

        writeAsicManifest(manifestBytes);

        // Sign the manifest
        SignatureHelper signatureHelper = new SignatureHelper(keyStoreFile, keyStorePassword, privateKeyPassword);
        writeSignature(signatureHelper.signData(manifestBytes));

        try {
            zipOutputStream.finish();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to finish the container. " + e.getMessage(), e);
        }
    }

    private void writeAsicManifest(byte[] manifestBytes) {
        try {
            zipOutputStream.putNextEntry(new ZipEntry("META-INF/asicmanifest.xml"));
            zipOutputStream.write(manifestBytes);
            zipOutputStream.closeEntry();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to create new ZIP entry " + e.getMessage(), e);
        }
    }

    private void writeSignature(byte[] signature) {
        try {
            zipOutputStream.putNextEntry(new ZipEntry("META-INF/signature.p7s"));
            zipOutputStream.write(signature);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create signature.p7s entry");
        }
    }

    private MessageDigest createMessageDigester() {
        try {
            messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm " + MESSAGE_DIGEST_ALGORITHM + " not supported");
        }
        return messageDigest;
    }

    /**
     * Adds the "mimetype" object to the archive
     */
    void putMimeTypeAsFirstEntry() {
        ZipEntry mimetypeEntry = new ZipEntry("mimetype");
        mimetypeEntry.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);
        mimetypeEntry.setMethod(ZipEntry.STORED);
        mimetypeEntry.setSize(APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes().length);
        CRC32 crc32 = new CRC32();
        crc32.update(APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes());
        mimetypeEntry.setCrc(crc32.getValue());
        try {
            zipOutputStream.putNextEntry(mimetypeEntry);
            zipOutputStream.write(APPLICATION_VND_ETSI_ASIC_E_ZIP.getBytes());
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create entry for 'mimetype'" + e.getMessage(), e);
        }
    }

    static ZipOutputStream createZipOutputStream(OutputStream outputStream) {
        ZipOutputStream zipOutputStream;
        zipOutputStream = new ZipOutputStream(outputStream);
        zipOutputStream.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);
        return zipOutputStream;
    }

    void transferFilesIntoArchive(Map<String, AsicDataObjectEntry> entries, MessageDigest messageDigester) {

        for (Map.Entry<String, AsicDataObjectEntry> entry : entries.entrySet()) {

            AsicDataObjectEntry entryInfo = entry.getValue();

            ZipEntry zipEntry = new ZipEntry(entryInfo.getName());  // Creates another zip entry
            try {
                zipOutputStream.putNextEntry(zipEntry);     // shoves the zip entry into the stream

                // Copies the entire contents of the file into the zip output stream
                byte[] digestBytes = copyFileToOutputStream(entryInfo.getFile(), messageDigester);

                // saves the message digest bytes
                entryInfo.setDigestBytes(digestBytes);

                zipOutputStream.closeEntry();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create new entry in archive: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Copies the entire contents of the supplied file to the ZIP outputstream. Calculating the Digest value as
     * the bytes are copied.
     *
     * @return the message digest bytes calculated during the transfer from the input to the outputstream
     */
    byte[] copyFileToOutputStream(File file, MessageDigest messageDigester) {

        messageDigester.reset();
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(zipOutputStream, messageDigester);

        BufferedInputStream bufferedInputStream = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            bufferedInputStream = new BufferedInputStream(fileInputStream);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = bufferedInputStream.read(buffer)) > 0) {
                zipOutputStreamWithDigest.write(buffer, 0, bytesRead);
            }

            zipOutputStreamWithDigest.flush();

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to open " + file + "; " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("I/O error while copying " + file + " into archive." + e.getMessage(), e);
        } finally {
            // Closes the input stream
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to close inputstream for " + file + "; " + e.getMessage(), e);
                }
            }
        }
        return messageDigester.digest();
    }

}
