package no.difi.asic;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

class AsicXadesWriter extends AsicAbstractWriter {

    public AsicXadesWriter(SignatureMethod signatureMethod, OutputStream outputStream, Path containerPath) {
        super(outputStream, containerPath, new AsicXadesManifest(signatureMethod.getMessageDigestAlgorithm()));
    }

    @Override
    void performSign(SignatureHelper signatureHelper) throws IOException {
        // Generate and write manifest (META-INF/signatures.xml)
        byte[] manifestBytes = ((AsicXadesManifest) asicManifest).toBytes(signatureHelper);
        writeZipEntry("META-INF/signatures.xml", manifestBytes);

        // System.out.println(new String(manifestBytes));
    }
}
