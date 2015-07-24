package no.difi.asic;

import java.io.IOException;
import java.io.OutputStream;

class XadesAsicWriter extends AbstractAsicWriter {

    public XadesAsicWriter(SignatureMethod signatureMethod, OutputStream outputStream, boolean closeStreamOnClose) throws IOException {
        super(outputStream, closeStreamOnClose, new XadesAsicManifest(signatureMethod.getMessageDigestAlgorithm()));
    }

    @Override
    public AsicWriter setRootFilename(String filename) {
        throw new IllegalStateException("ASiC-E XAdES does not support defining root file.");
    }

    @Override
    void performSign(SignatureHelper signatureHelper) throws IOException {
        // Generate and write manifest (META-INF/signatures.xml)
        byte[] manifestBytes = ((XadesAsicManifest) asicManifest).toBytes(signatureHelper);
        asicOutputStream.writeZipEntry("META-INF/signatures.xml", manifestBytes);

        // System.out.println(new String(manifestBytes));
    }
}
