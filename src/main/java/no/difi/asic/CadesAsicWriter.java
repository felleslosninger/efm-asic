package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Builds an ASiC-E Cades container using a variation of "builder pattern".
 *
 * This class is not thread safe, as it indirectly holds a MessageDigest object.
 *
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.09
 */
class CadesAsicWriter extends AbstractAsicWriter {

    public static final Logger log = LoggerFactory.getLogger(CadesAsicWriter.class);

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public CadesAsicWriter(SignatureMethod signatureMethod, OutputStream outputStream, Path containerPath) {
        super(outputStream, containerPath, new CadesAsicManifest(signatureMethod.getMessageDigestAlgorithm()));
    }

    @Override
    protected void performSign(SignatureHelper signatureHelper) throws IOException {
        // Adding signature file to asic manifest before actual signing
        ((CadesAsicManifest) asicManifest).setSignature("META-INF/signature.p7s", "application/x-pkcs7-signature");

        // Generates and writes manifest (META-INF/asicmanifest.xml) to the zip archive
        byte[] manifestBytes = ((CadesAsicManifest) asicManifest).toBytes();
        writeZipEntry("META-INF/asicmanifest.xml", manifestBytes);

        // Generates and writes signature (META-INF/signature.p7s) to the zip archive
        writeZipEntry("META-INF/signature.p7s", signatureHelper.signData(manifestBytes));
    }
}