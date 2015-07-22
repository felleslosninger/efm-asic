package no.difi.asic;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

class XadesAsicWriter extends AbstractAsicWriter {

    public XadesAsicWriter(SignatureMethod signatureMethod, OutputStream outputStream, Path containerPath) {
        super(outputStream, containerPath, new XadesAsicManifest(signatureMethod.getMessageDigestAlgorithm()));
    }

    @Override
    void performSign(SignatureHelper signatureHelper) throws IOException {
        // Generate and write manifest (META-INF/signatures.xml)
        byte[] manifestBytes = ((XadesAsicManifest) asicManifest).toBytes(signatureHelper);
        writeZipEntry("META-INF/signatures.xml", manifestBytes);

        // System.out.println(new String(manifestBytes));
    }
}
