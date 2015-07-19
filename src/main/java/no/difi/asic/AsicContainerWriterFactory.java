package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicContainerWriterFactory {

    private SignatureMethod signatureMethod;

    public AsicContainerWriterFactory(SignatureMethod signatureMethod) {
        this.signatureMethod = signatureMethod;
    }

    // Helper method
    public IAsicContainerWriter newInstance(File outputDir, String filename) throws IOException {
        return newInstance(new File(outputDir, filename));
    }

    // Helper method
    public IAsicContainerWriter newInstance(File file) throws IOException {
        return newInstance(file.toPath());
    }

    // Helper method
    public IAsicContainerWriter newInstance(Path path) throws IOException {
        return newInstance(Files.newOutputStream(path), path);
    }

    // Helper method
    public IAsicContainerWriter newInstance(OutputStream outputStream) throws IOException {
        return newInstance(outputStream, null);
    }

    protected IAsicContainerWriter newInstance(OutputStream outputStream, Path file) {
        switch (signatureMethod) {
            case CAdES:
                return new AsicCadesContainerWriter(outputStream, file);
            default:
                throw new IllegalStateException(String.format("Not implemented: %s", signatureMethod));
        }
    }
}
