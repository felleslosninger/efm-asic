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
    public IAsicContainerWriter newContainer(File outputDir, String filename) throws IOException {
        return newContainer(new File(outputDir, filename));
    }

    // Helper method
    public IAsicContainerWriter newContainer(File file) throws IOException {
        return newContainer(file.toPath());
    }

    // Helper method
    public IAsicContainerWriter newContainer(Path path) throws IOException {
        return newContainer(Files.newOutputStream(path), path);
    }

    // Helper method
    public IAsicContainerWriter newContainer(OutputStream outputStream) throws IOException {
        return newContainer(outputStream, null);
    }

    protected IAsicContainerWriter newContainer(OutputStream outputStream, Path file) {
        switch (signatureMethod) {
            case CAdES:
                return new AsicCadesContainerWriter(outputStream, file);
            case XAdES:
                return new AsicXadesContainerWriter(outputStream, file);
            default:
                throw new IllegalStateException(String.format("Not implemented: %s", signatureMethod));
        }
    }
}
