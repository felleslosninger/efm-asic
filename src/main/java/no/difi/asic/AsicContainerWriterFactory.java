package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicContainerWriterFactory {

    public static AsicContainerWriterFactory newFactory(SignatureMethod signatureMethod) {
        return new AsicContainerWriterFactory(signatureMethod);
    }

    private SignatureMethod signatureMethod;

    private AsicContainerWriterFactory(SignatureMethod signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    // Helper method
    public AsicContainerWriter newContainer(File outputDir, String filename) throws IOException {
        return newContainer(new File(outputDir, filename));
    }

    // Helper method
    public AsicContainerWriter newContainer(File file) throws IOException {
        return newContainer(file.toPath());
    }

    // Helper method
    public AsicContainerWriter newContainer(Path path) throws IOException {
        return newContainer(Files.newOutputStream(path), path);
    }

    // Helper method
    public AsicContainerWriter newContainer(OutputStream outputStream) throws IOException {
        return newContainer(outputStream, null);
    }

    protected AsicContainerWriter newContainer(OutputStream outputStream, Path file) {
        switch (signatureMethod) {
            case CAdES:
                return new AsicCadesContainerWriter(signatureMethod, outputStream, file);
            case XAdES:
                return new AsicXadesContainerWriter(signatureMethod, outputStream, file);
            default:
                throw new IllegalStateException(String.format("Not implemented: %s", signatureMethod));
        }
    }
}
