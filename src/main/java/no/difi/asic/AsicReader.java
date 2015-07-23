package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicReader extends AbstractAsicReader {

    AsicReader(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
        super(messageDigestAlgorithm, inputStream);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNextFile() throws IOException {
        return super.getNextFile();
    }

    /**
     * Writes the contents of the current entry into a file
     * @param file into which the contents should be written.
     * @throws IOException
     */
    public void writeFile(File file) throws IOException {
        writeFile(file.toPath());
    }

    /**
     * Writes contents of current archive entry into a file.
     * @param path into which the contents of current entry should be written.
     * @throws IOException
     */
    public void writeFile(Path path) throws IOException {
        OutputStream outputStream = Files.newOutputStream(path);
        writeFile(outputStream);
        outputStream.close();
    }

    /**
     * {@inheritDoc}
     * @param outputStream into which data from current entry should be written.
     * @throws IOException
     */
    @Override
    public void writeFile(OutputStream outputStream) throws IOException {
        super.writeFile(outputStream);
    }

    /**
     * {@inheritDoc}
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        super.close();
    }

}
