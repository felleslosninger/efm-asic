package no.difi.asic;

import com.sun.xml.bind.api.JAXBRIContext;
import no.difi.xsd.asic.model._1.AsicFile;
import no.difi.xsd.asic.model._1.AsicManifest;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._2918.v1_2.ASiCManifestType;
import org.etsi.uri._2918.v1_2.DataObjectReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

class AbstractAsicReader {

    private static final Logger log = LoggerFactory.getLogger(AbstractAsicReader.class);

    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBRIContext.newInstance(ASiCManifestType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    protected MessageDigestAlgorithm messageDigestAlgorithm;
    protected MessageDigest messageDigest;

    protected AsicInputStream zipInputStream;
    protected ZipEntry zipEntry;

    private AsicManifest asicManifest = new AsicManifest();
    private Map<String, AsicFile> asicManifestMap = new HashMap<>();

    AbstractAsicReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        this.messageDigestAlgorithm = messageDigestAlgorithm;

        try {
            messageDigest = MessageDigest.getInstance(this.messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()));
        }

        zipInputStream = new AsicInputStream(inputStream);
        // Comment in ZIP is stored in Central Directory in the end of the file.
    }

    protected String getNextFile() throws IOException {
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            log.info(String.format("Found file: %s", zipEntry.getName()));

            // Files used for validation is not exposed
            if (!specialFile()) {
                // Adding file to internal manifest if not there.
                if (!asicManifestMap.containsKey(zipEntry.getName())) {
                    AsicFile asicFile = new AsicFile();
                    asicFile.setName(zipEntry.getName());

                    asicManifest.getFile().add(asicFile);
                    asicManifestMap.put(zipEntry.getName(), asicFile);
                }

                return zipEntry.getName();
            }
        }

        return null;
    }

    protected void writeFile(OutputStream outputStream) throws IOException {
        if (zipEntry == null)
            throw new IllegalStateException("No file to read.");

        // Calculate digest while reading file
        messageDigest.reset();
        DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
        IOUtils.copy(zipInputStream, digestOutputStream);

        zipInputStream.closeEntry();

        // Get digest
        byte[] digest = messageDigest.digest();
        log.debug(String.format("Digest: %s", new String(Base64.encode(digest))));

        // Fetch new or old asicFile
        AsicFile asicFile = asicManifestMap.get(zipEntry.getName());

        // Check digest if asicFile has a digest
        if (asicFile.getDigest() != null) {
            // Throw exception if calculated digest doesn't match manifest
            if (Arrays.equals(digest, asicFile.getDigest()))
                throw new IllegalStateException(String.format("Mismatching digest for file %s", zipEntry.getName()));

            // Passed test, mark file as verified
            asicFile.setVerified(true);
        }

        // Attach digest
        asicFile.setDigest(digest);
    }

    protected void close() throws IOException {
        if (zipInputStream != null) {
            zipInputStream.close();
            zipInputStream = null;
        }
    }

    protected boolean specialFile() throws IOException {
        if (!zipEntry.getName().startsWith("META-INF/"))
            return false;

        String filename = zipEntry.getName().substring(9).toLowerCase();

        // Handling manifest in ASiC CAdES.
        if (filename.startsWith("asicmanifest")) {
            // Read content in manifest (also used for verification of signature)
            ByteArrayOutputStream manifestStream = new ByteArrayOutputStream();
            IOUtils.copy(zipInputStream, manifestStream);

            try {
                // Read XML
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                ASiCManifestType manifest = ((JAXBElement<ASiCManifestType>) unmarshaller.unmarshal(new ByteArrayInputStream(manifestStream.toByteArray()))).getValue();

                // Run through recorded objects
                for (DataObjectReferenceType referenceType : manifest.getDataObjectReference()) {
                    // Make sure digest algorithm is correct
                    if (!messageDigestAlgorithm.getUri().equals(referenceType.getDigestMethod().getAlgorithm()))
                        throw new IllegalStateException(String.format("Wrong digest method for file %s: %s", referenceType.getURI(), referenceType.getDigestMethod().getAlgorithm()));

                    // Fetch file from internal manifest
                    AsicFile asicFile = asicManifestMap.get(referenceType.getURI());
                    if (asicFile == null) {
                        // Add file to internal manifest
                        asicFile = new AsicFile();
                        asicFile.setName(referenceType.getURI());
                        asicFile.setDigest(referenceType.getDigestValue());
                        asicFile.setMimetype(referenceType.getMimeType());

                        asicManifest.getFile().add(asicFile);
                        asicManifestMap.put(asicFile.getName(), asicFile);
                    } else {
                        // Add mimetype to file
                        asicFile.setMimetype(referenceType.getMimeType());

                        // Throw exception if calculated digest doesn't match manifest
                        if (!Arrays.equals(asicFile.getDigest(), referenceType.getDigestValue()))
                            throw new IllegalStateException(String.format("Mismatching digest for file %s", referenceType.getURI()));

                        // Passed test, mark file as verified
                        asicFile.setVerified(true);
                    }
                }
            } catch (JAXBException e) {
                log.error(String.format("Unable to read content in %s.", zipEntry.getName()));
            }
            return true;
        }

        if (filename.startsWith("signature"))
            return true;

        return false;
    }

}
