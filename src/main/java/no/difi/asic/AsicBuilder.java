package no.difi.asic;

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
public class AsicBuilder {

    public static final Logger log = LoggerFactory.getLogger(AsicBuilder.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";

    public static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";

    // Holds the list of entries to be added to the container
    private Map<String, AsicDataObjectEntry> files = new HashMap<String, AsicDataObjectEntry>();

    /**
     * Holds the archiveName of container, which may not contain special characters like for instance ':', '/', '\\'
     */
    private String archiveName;

    /** Directory into which the ASiC container should be written */
    private File outputDir;

    private MessageDigest messageDigester;
    private final JAXBContext jaxbContext;

    private File keyStoreFile;
    private String keyStorePassword;
    private String privateKeyPassword;

    public AsicBuilder() {

        try {
            // Creating the JAXBContext is heavy lifting, so do it only once.
            jaxbContext = JAXBContext.newInstance(ASiCManifestType.class);
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
    public AsicBuilder addFile(File file) {
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
    public AsicBuilder addFile(File fileReference, String entryName) {

        String mimeTypeStr = null;
        MimeType mimeType = null;
        URI uri = null;
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


    public AsicBuilder keyStore(File keyStoreResourceName) {
        this.keyStoreFile = keyStoreResourceName;
        return this;
    }

    public AsicBuilder keyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public AsicBuilder privateKeyPassword(String privateKeyPassword) {
        this.privateKeyPassword = privateKeyPassword;
        return this;
    }

    /**
     * Provides a map with the details about each data object that has been added to the container.
     * @return
     */
    public Map<String, AsicDataObjectEntry> getFiles() {
        return files;
    }

    /**
     * Property getter for the ASiC archive file name.
     *
     * @return
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * Property getter for directory into which the ASiC container will be written.
     *
     * @return
     */
    public File getOutputDir() {
        return outputDir;
    }


    /**
     * Identifies the output directory into which the archive will be created.
     *
     */
    public AsicBuilder outputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    /** The file name of the ASiC container.
     * If an extension is given like for instance <code>.zip</code> it will override
     * the default extension of either <code>.asice</code> or <code>.sce</code>
     *
     */
    public AsicBuilder archiveName(String name) {
        // TODO: check the extension of the name and add .asic or .sce if no extension has been provided.
        this.archiveName = name;

        // TODO: verify there are no special characters in the archiveName

        return this;
    }

    /**
     * Builds the ASiC container, i.e. creates the ZIP file.
     *
     * @return
     */
    public AsicContainer build() {

        // Creates the messages digester for each file added to the container.
        messageDigester = createMessageDigester();


        if (getOutputDir() == null) {
            throw new IllegalStateException("Output directory for the ASiC container not specified");
        }
        if (getArchiveName() == null) {
            throw new IllegalStateException("Name of archive not specified");
        }
        if (keyStoreFile == null) {
            throw new IllegalStateException("KeyStore file not specified");
        }
        if (keyStorePassword == null) {
            throw new IllegalStateException("Password for the keystore must be supplied");
        }
        if (privateKeyPassword == null) {
            throw new IllegalStateException("Password for private key within keystore must be supplied");
        }

        File outputFile = computeFileName(getOutputDir(), getArchiveName());

        ZipOutputStream zipOutputStream = createZipOutputStream(outputFile);

        putMimeTypeAsFirstEntry(zipOutputStream);

        // Adds all the files, computing the digest values for each one as we iterate over them
        transferFilesIntoArchive(zipOutputStream, getFiles(),messageDigester);

        // Creates the ASicManiFest and shoves it into the ZipFile
        AsicManifestReference asicManifestReference = new AsicManifestReference(getFiles().values());
        byte[] manifestBytes = asicManifestReference.toBytes(jaxbContext);

        writeAsicManifest(zipOutputStream, manifestBytes);

        // Sign the manifest
        SignatureHelper signatureHelper = new SignatureHelper(keyStoreFile, keyStorePassword, privateKeyPassword);
        byte[] signature = signatureHelper.signData(manifestBytes);

        try {
            zipOutputStream.putNextEntry(new ZipEntry("META-INF/signature.p7s"));
            zipOutputStream.write(signature);
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create signature.p7s entry");
        }
        try {
            zipOutputStream.finish();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to finish the container. " + e.getMessage(), e);
        }

        return new AsicContainer(outputFile);
    }

    private void writeAsicManifest(ZipOutputStream zipOutputStream, byte[] manifestBytes) {
        try {
            zipOutputStream.putNextEntry(new ZipEntry("META-INF/asicmanifest.xml"));
            zipOutputStream.write(manifestBytes, 0, manifestBytes.length);
            zipOutputStream.closeEntry();

        } catch (IOException e) {
            throw new IllegalStateException("Unable to create new ZIP entry " + e.getMessage(), e);
        }
    }

    private MessageDigest createMessageDigester() {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm " + MESSAGE_DIGEST_ALGORITHM + " not supported");
        }
        return messageDigest;
    }


    static File computeFileName(File outputDirectory, String archiveName) {

        return new File(outputDirectory, archiveName);
    }

    /**
     * Adds the "mimetype" object to the archive
     */
    static void putMimeTypeAsFirstEntry(ZipOutputStream zipOutputStream) {
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

    static ZipOutputStream createZipOutputStream(File file) {
        ZipOutputStream zipOutputStream;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
            zipOutputStream.setComment("mimetype=" + APPLICATION_VND_ETSI_ASIC_E_ZIP);

        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Unable to create output file " + file, e);
        }
        return zipOutputStream;
    }



    /**
     * @param zipOutputStream
     * @param entries
     * @return Map of zipEntryNames and their corresponding digest bytes
     */
    static void transferFilesIntoArchive(ZipOutputStream zipOutputStream, Map<String, AsicDataObjectEntry> entries, MessageDigest messageDigester) {

        for (Map.Entry<String, AsicDataObjectEntry> entry : entries.entrySet()) {

            AsicDataObjectEntry entryInfo = entry.getValue();

            ZipEntry zipEntry = new ZipEntry(entryInfo.getName());  // Creates another zip entry
            try {
                zipOutputStream.putNextEntry(zipEntry);     // shoves the zip entry into the stream

                // Copies the entire contents of the file into the zip output stream
                byte[] digestBytes = copyFileToOutputStream(entryInfo.getFile(), zipOutputStream, messageDigester);

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
    static byte[] copyFileToOutputStream(File file, ZipOutputStream zipOutputStream, MessageDigest messageDigester) {

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
