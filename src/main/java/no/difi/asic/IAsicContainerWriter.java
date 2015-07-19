package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface IAsicContainerWriter {
    // Helper method
    IAsicContainerWriter add(File file) throws IOException;

    // Helper method
    IAsicContainerWriter add(File file, String filename) throws IOException;

    // Helper method
    IAsicContainerWriter add(Path path) throws IOException;

    // Helper method
    IAsicContainerWriter add(Path path, String filename) throws IOException;

    IAsicContainerWriter add(InputStream inputStream, String filename) throws IOException;

    // Helper method
    IAsicContainerWriter add(File file, String filename, String mimeType) throws IOException;

    // Helper method
    IAsicContainerWriter add(Path path, String filename, String mimeType) throws IOException;

    IAsicContainerWriter add(InputStream inputStream, String filename, String mimeType) throws IOException;

    // Helper method
    IAsicContainerWriter sign(File keyStoreResourceName, String keyStorePassword, String keyPassword) throws IOException;

    IAsicContainerWriter sign(File keyStoreResourceName, String keyStorePassword, String keyAlias, String keyPassword) throws IOException;

    IAsicContainerWriter sign(SignatureHelper signatureHelper) throws IOException;

    File getContainerFile();

    Path getContainerPath();
}
