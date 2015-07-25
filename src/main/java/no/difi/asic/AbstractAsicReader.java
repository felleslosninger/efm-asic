package no.difi.asic;

import no.difi.xsd.asic.model._1.AsicManifest;
import no.difi.xsd.asic.model._1.Certificate;
import oasis.names.tc.opendocument.xmlns.manifest._1.Manifest;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;

/**
 * Skeleton implementation of ASiC archive reader.
 *
 * @author Erlend Klakegg Bergheim
 */
class AbstractAsicReader {

    private static final Logger log = LoggerFactory.getLogger(AbstractAsicReader.class);

    private MessageDigest messageDigest;

    private AsicInputStream zipInputStream;
    private ZipEntry currentZipEntry;

    private ManifestVerifier manifestVerifier;
    private Manifest manifest;

    /**
     * Used to hold signature or manifest for CAdES as they are not in the same file.
     */
    private Map<String, Object> signingContent = new HashMap<>();

    AbstractAsicReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        this.manifestVerifier = new ManifestVerifier(messageDigestAlgorithm);

        try {
            messageDigest = MessageDigest.getInstance(messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()), e);
        }

        zipInputStream = new AsicInputStream(inputStream);
        // Comment in ZIP is stored in Central Directory in the end of the file.
    }

    /**
     * Provides the name of the next entry in the ASiC archive and positions the inputstream at the beginning of the data.
     *
     * @return name of next entry in archive.
     * @throws IOException
     */
    String getNextFile() throws IOException {
        while ((currentZipEntry = zipInputStream.getNextEntry()) != null) {
            log.info(String.format("Found file: %s", currentZipEntry.getName()));

            // Files used for validation are not exposed
            if (currentZipEntry.getName().startsWith("META-INF/"))
                handleMetadataEntry();
            else
                return currentZipEntry.getName();
        }

        // Making sure signatures are used and all files are signed after reading all content.

        // All files must be signed by minimum one manifest/signature.
        manifestVerifier.verifyAllVerified();

        // All CAdES signatures and manifest must be verified.
        if (signingContent.size() > 0)
            throw new IllegalStateException(String.format("Signature not verified: %s", signingContent.keySet().iterator().next()));

        // Return null when container is out of content to read.
        return null;
    }

    /**
     * Writes contents of current archive entry to the supplied output stream.
     * @param outputStream into which data from current entry should be written.
     * @throws IOException
     */
    void writeFile(OutputStream outputStream) throws IOException {
        if (currentZipEntry == null)
            throw new IllegalStateException("No file to read.");

        // Calculate digest while reading file
        messageDigest.reset();
        DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
        IOUtils.copy(zipInputStream, digestOutputStream);

        zipInputStream.closeEntry();

        // Get digest
        byte[] digest = messageDigest.digest();
        log.debug(String.format("Digest: %s", new String(Base64.encode(digest))));

        manifestVerifier.update(currentZipEntry.getName(), digest, null);
    }

    /**
     * Closes the underlying zip input stream.
     *
     * @throws IOException
     */
    void close() throws IOException {
        if (zipInputStream != null) {
            zipInputStream.close();
            zipInputStream = null;
        }
    }

    /**
     * Handles zip entries in the META-INF/ directory.
     *
     * @throws IOException
     */
    private void handleMetadataEntry() throws IOException {
        // Extracts everything after META-INF/
        String filename = currentZipEntry.getName().substring(9).toLowerCase();

        // Read content in file
        ByteArrayOutputStream contentsOfStream = new ByteArrayOutputStream();
        IOUtils.copy(zipInputStream, contentsOfStream);

        if (filename.startsWith("asicmanifest")) {
            // Handling manifest in ASiC CAdES.
            String sigReference = CadesAsicManifest.extractAndVerify(contentsOfStream.toString(), manifestVerifier);
            handleCadesSigning(sigReference, contentsOfStream.toString());
        } else if (filename.startsWith("signature") && filename.endsWith(".xml")) {
            // Handling manifest in ASiC XAdES.
            XadesAsicManifest.extractAndVerify(contentsOfStream.toString(), manifestVerifier);
        } else if (filename.startsWith("signature") && filename.endsWith(".p7s")) {
            // Handling signature in ASiC CAdES.
            handleCadesSigning(currentZipEntry.getName(), contentsOfStream);
        } else if (filename.equals("manifest.xml")) {
            // Read manifest.
            manifest = OasisManifest.read(new ByteArrayInputStream(contentsOfStream.toByteArray()));
        } else {
            throw new IllegalStateException(String.format("Contains unknown metadata file: %s", currentZipEntry.getName()));
        }
    }

    private void handleCadesSigning(String sigReference, Object o) {
        if (!signingContent.containsKey(sigReference))
            signingContent.put(sigReference, o);
        else {
            byte[] data = o instanceof String ? ((String) o).getBytes() : ((String) signingContent.get(sigReference)).getBytes();
            byte[] sign = o instanceof ByteArrayOutputStream ? ((ByteArrayOutputStream) o).toByteArray() : ((ByteArrayOutputStream) signingContent.get(sigReference)).toByteArray();

            Certificate certificate = SignatureHelper.validate(data, sign);
            certificate.setCert(currentZipEntry.getName());
            manifestVerifier.addCertificate(certificate);

            signingContent.remove(sigReference);
        }
    }

    /**
     * Property getter for the AsicManifest of the ASiC archive.
     *
     * @return value of property.
     */
    public AsicManifest getAsicManifest() {
        return manifestVerifier.getAsicManifest();
    }

    /**
     * Property getter for the OpenDocument manifest.
     *
     * @return value of property, null if document is not found in container.
     */
    public Manifest getOasisManifest() {
        return manifest;
    }

}
