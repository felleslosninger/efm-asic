package no.difi.asic;

import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.etsi.uri._2918.v1_1.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
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
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.44
 */
public class AsicContainer {

    public static final Logger log = LoggerFactory.getLogger(AsicContainer.class);

    public static final String APPLICATION_VND_ETSI_ASIC_E_ZIP = "application/vnd.etsi.asic-e+zip";
    public static final String SHA_256 = "SHA-256";


    /**
     * Holds the file reference for the ASiC container
     */
    private File file;
    /**
     * Message digester used for calculating digests.
     */
    private final MessageDigest messageDigest;

    public File getFile() {
        return file;
    }

    public AsicContainer(AsicBuilder asicBuilder) {

        messageDigest = createMessageDigest();


        if (asicBuilder.getOutputDir() == null) {
            throw new IllegalStateException("Output directory for the ASiC container not specified");
        }
        if (asicBuilder.getArchiveName() == null) {
            throw new IllegalStateException("Name of archive not specified");
        }

        file = computeFileName(asicBuilder.getOutputDir(), asicBuilder.getArchiveName());

        ZipOutputStream zipOutputStream = createZipOutputStream(file);

        putMimeTypeAsFirstEntry(zipOutputStream);

        // Adds all the files
        Map<String, byte[]> entryNamesAndDigests = transferFilesIntoArchive(zipOutputStream, asicBuilder.getFiles());

        // Creates the ASicManiFest and shoves it into the ZipFile
        AsicManifestReference.Builder manifestBuilder = new AsicManifestReference.Builder();

        for (Map.Entry<String, File> fileEntry : asicBuilder.getFiles().entrySet()) {
            File f = fileEntry.getValue();
            byte[] digestBytes = entryNamesAndDigests.get(fileEntry.getKey());

            try {
                String mimeType = Files.probeContentType(f.toPath());
                URI uri = new URI(fileEntry.getKey());
                manifestBuilder.addDataObjectReference(uri, mimeType, digestBytes);
            } catch (IOException e) {
                log.warn("Unable to determine MIME type of file " + f + "; " + e.getMessage(), e);
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Can not convert entry " + fileEntry.getKey() + " to URI.", e);
            }
        }

        AsicManifestReference asicManifestReference = manifestBuilder.build();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ASiCManifestType.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ObjectFactory objectFactory = new ObjectFactory();

            zipOutputStream.putNextEntry(new ZipEntry("META-INF/asicmanifest.xml"));
            JAXBElement<ASiCManifestType> jaxbRootElement = objectFactory.createASiCManifest(asicManifestReference.getaSiCManifestType());
            marshaller.marshal(jaxbRootElement, zipOutputStream);

            zipOutputStream.closeEntry();

        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create JAXBContext " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create new ZIP entry " + e.getMessage(), e);
        }


        try {
            zipOutputStream.finish();
            zipOutputStream.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to finish the container. " + e.getMessage(), e);
        }
    }

    private MessageDigest createMessageDigest() {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(SHA_256);
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm " + SHA_256 + " not supported");
        }
        return messageDigest;
    }

    /**
     * @param zipOutputStream
     * @param entries
     * @return Map of zipEntryNames and their corresponding digest bytes
     */
    Map<String, byte[]> transferFilesIntoArchive(ZipOutputStream zipOutputStream, Map<String, File> entries) {

        Map<String, byte[]> digests = new HashMap<String, byte[]>();

        for (Map.Entry<String, File> entry : entries.entrySet()) {

            String zipEntryName = entry.getKey();
            File file = entry.getValue();

            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            try {
                zipOutputStream.putNextEntry(zipEntry);
                // Copies the entire contents of the file into the zip output stream
                byte[] digestBytes = copyFileToOutputStream(file, zipOutputStream);
                // saves the message digest bytes
                digests.put(zipEntryName, digestBytes);

                zipOutputStream.closeEntry();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create new entry in archive: " + e.getMessage(), e);
            }

        }

        return digests;
    }

    /**
     * Copies the entire contents of the supplied file to the ZIP outputstream. Calculating the Digest value as
     * the bytes are copied.
     *
     * @return the message digest bytes calculated during the transfer from the input to the outputstream
     */
    byte[] copyFileToOutputStream(File file, ZipOutputStream zipOutputStream) {

        messageDigest.reset();
        DigestOutputStream zipOutputStreamWithDigest = new DigestOutputStream(zipOutputStream, messageDigest);

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
        return messageDigest.digest();
    }

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

    static File computeFileName(File outputDirectory, String archiveName) {

        return new File(outputDirectory, archiveName);
    }
}
