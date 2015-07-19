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
class AsicCadesContainerWriter extends AsicAbstractContainerWriter {

    public static final Logger log = LoggerFactory.getLogger(AsicCadesContainerWriter.class);

    /**
     * Prepares creation of a new container.
     * @param outputStream Stream used to write container.
     */
    public AsicCadesContainerWriter(SignatureMethod signatureMethod, OutputStream outputStream, Path containerPath) {
        super(outputStream, containerPath, new AsicCadesManifest(signatureMethod.getMessageDigestAlgorithm()));
    }

    @Override
    protected void performSign(SignatureHelper signatureHelper) throws IOException {
        // Adding signature file to manifest before signing
        ((AsicCadesManifest) asicManifest).setSignature("META-INF/signature.p7s", "application/x-pkcs7-signature");

        // Generate and write manifest (META-INF/asicmanifest.xml)
        byte[] manifestBytes = ((AsicCadesManifest) asicManifest).toBytes();
        writeZipEntry("META-INF/asicmanifest.xml", manifestBytes);

        // Generate and write signature (META-INF/signature.p7s)
        writeZipEntry("META-INF/signature.p7s", signatureHelper.signData(manifestBytes));
    }
}
