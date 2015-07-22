package no.difi.asic;

import com.sun.xml.bind.api.JAXBRIContext;
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

    protected ManifestVerifier manifestVerifier;

    AbstractAsicReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
        this.manifestVerifier = new ManifestVerifier(this.messageDigestAlgorithm);

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
            if (!specialFile())
                return zipEntry.getName();
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

        manifestVerifier.update(zipEntry.getName(), digest);
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
                for (DataObjectReferenceType referenceType : manifest.getDataObjectReference())
                    manifestVerifier.update(referenceType.getURI(), referenceType.getMimeType(), referenceType.getDigestValue(), referenceType.getDigestMethod().getAlgorithm());
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
