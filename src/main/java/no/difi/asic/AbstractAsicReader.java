package no.difi.asic;

import com.google.common.io.ByteStreams;
import no.difi.commons.asic.jaxb.asic.AsicManifest;
import no.difi.commons.asic.jaxb.asic.Certificate;
import no.difi.commons.asic.jaxb.opendocument.manifest.Manifest;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.DigestInputStream;
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
abstract class AbstractAsicReader implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractAsicReader.class);

    private MessageDigest messageDigest;

    private AsicInputStream zipInputStream;
    private ZipEntry currentZipEntry;

    private ManifestVerifier manifestVerifier;
    private Manifest manifest;

    // Initiated with 'true' as the first file should not do anything.
    private boolean contentIsWritten = true;

    private SignatureVerifier signatureVerifier = new SignatureVerifier();

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

    public String getNextFile() throws IOException {
        // Read file if the user didn't.
        if (!contentIsWritten)
            writeFile(ByteStreams.nullOutputStream());

        // Write digest to manifest
        if (currentZipEntry != null) {
            byte[] digest = messageDigest.digest();
            logger.debug("Digest: {}", Base64.encode(digest));
            manifestVerifier.update(currentZipEntry.getName(), digest, null);
        }

        while ((currentZipEntry = zipInputStream.getNextEntry()) != null) {
            logger.info("Found file: {}", currentZipEntry.getName());

            // Files used for validation are not exposed
            if (currentZipEntry.getName().startsWith("META-INF/"))
                handleMetadataEntry();
            else {
                contentIsWritten = false;
                return currentZipEntry.getName();
            }
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

    void writeFile(OutputStream outputStream) throws IOException {
        if (currentZipEntry == null)
            throw new IllegalStateException("No file to read.");

        // Calculate digest while reading file
        messageDigest.reset();
        DigestOutputStream digestOutputStream = new DigestOutputStream(outputStream, messageDigest);
        ByteStreams.copy(zipInputStream, digestOutputStream);

        zipInputStream.closeEntry();

        contentIsWritten = true;
    }

    InputStream inputStream() {
        if (currentZipEntry == null)
            throw new IllegalStateException("No file to read.");

        // We must trust the user.
        contentIsWritten = true;

        messageDigest.reset();
        return new InputStreamWrapper(new DigestInputStream(zipInputStream, messageDigest));
    }

    @Override
    public void close() throws IOException {
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
        ByteStreams.copy(zipInputStream, contentsOfStream);

        if (AsicUtils.PATTERN_CADES_MANIFEST.matcher(currentZipEntry.getName()).matches()) {
            // Handling manifest in ASiC CAdES.
            String sigReference = CadesAsicManifest.extractAndVerify(contentsOfStream.toString(), manifestVerifier);
            handleCadesSigning(sigReference, contentsOfStream.toString());
        } else if (AsicUtils.PATTERN_XADES_SIGNATURES.matcher(currentZipEntry.getName()).matches()) {
            // Handling manifest in ASiC XAdES.
            XadesAsicManifest.extractAndVerify(contentsOfStream.toString(), manifestVerifier);
        } else if (AsicUtils.PATTERN_CADES_SIGNATURE.matcher(currentZipEntry.getName()).matches()) {
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

            Certificate certificate = signatureVerifier.validate(data, sign);
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

    public void setSignatureVerifier(SignatureVerifier verifier) {
        this.signatureVerifier = verifier;
    }
}
