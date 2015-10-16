package no.difi.asic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class AsicReaderImpl extends AbstractAsicReader implements AsicReader {

    AsicReaderImpl(MessageDigestAlgorithm messageDigestAlgorithm, InputStream inputStream) throws IOException {
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
     * {@inheritDoc}
     */
    @Override
    public void writeFile(File file) throws IOException {
        writeFile(file.toPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFile(Path path) throws IOException {
        OutputStream outputStream = Files.newOutputStream(path);
        writeFile(outputStream);
        outputStream.close();
    }

    /**
     * {@inheritDoc}
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
