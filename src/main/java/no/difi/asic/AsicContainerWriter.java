package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface AsicContainerWriter {
    // Helper method
    AsicContainerWriter add(File file) throws IOException;

    // Helper method
    AsicContainerWriter add(File file, String filename) throws IOException;

    // Helper method
    AsicContainerWriter add(Path path) throws IOException;

    // Helper method
    AsicContainerWriter add(Path path, String filename) throws IOException;

    AsicContainerWriter add(InputStream inputStream, String filename) throws IOException;

    // Helper method
    AsicContainerWriter add(File file, String filename, String mimeType) throws IOException;

    // Helper method
    AsicContainerWriter add(Path path, String filename, String mimeType) throws IOException;

    AsicContainerWriter add(InputStream inputStream, String filename, String mimeType) throws IOException;

    // Helper method
    AsicContainerWriter sign(File keyStoreResourceName, String keyStorePassword, String keyPassword) throws IOException;

    AsicContainerWriter sign(File keyStoreResourceName, String keyStorePassword, String keyAlias, String keyPassword) throws IOException;

    AsicContainerWriter sign(SignatureHelper signatureHelper) throws IOException;

    File getContainerFile();

    Path getContainerPath();
}
