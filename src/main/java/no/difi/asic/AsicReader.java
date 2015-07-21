package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicReader extends AsicAbstractContainerReader {

    AsicReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        super(messageDigestAlgorithm, inputStream);
    }

    @Override
    public String getNextFile() throws IOException {
        return super.getNextFile();
    }

    // Helper method
    public void writeFile(File file) throws IOException {
        writeFile(file.toPath());
    }

    // Helper method
    public void writeFile(Path path) throws IOException {
        OutputStream outputStream = Files.newOutputStream(path);
        writeFile(outputStream);
        outputStream.close();
    }

    @Override
    public void writeFile(OutputStream outputStream) throws IOException {
        super.writeFile(outputStream);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
