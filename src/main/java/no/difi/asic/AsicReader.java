package no.difi.asic;

import no.difi.xsd.asic.model._1.AsicManifest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

public interface AsicReader {

    /**
     * Provides the name of the next entry in the ASiC archive and positions the inputstream at the beginning of the data.
     *
     * @return name of next entry in archive.
     * @throws IOException
     */
    String getNextFile() throws IOException;

    /**
     * Writes the contents of the current entry into a file
     * @param file into which the contents should be written.
     * @throws IOException
     */
    void writeFile(File file) throws IOException;

    /**
     * Writes contents of current archive entry into a file.
     * @param path into which the contents of current entry should be written.
     * @throws IOException
     */
    void writeFile(Path path) throws IOException;

    /**
     * Writes contents of current archive entry to the supplied output stream.
     * @param outputStream into which data from current entry should be written.
     * @throws IOException
     */
    void writeFile(OutputStream outputStream) throws IOException;

    /**
     * Closes the underlying zip input stream.
     *
     * @throws IOException
     */
    void close() throws IOException;

    AsicManifest getAsicManifest();
}
