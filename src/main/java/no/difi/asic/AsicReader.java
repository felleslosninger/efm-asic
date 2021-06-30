package no.difi.asic;

import no.difi.commons.asic.jaxb.asic.AsicManifest;
import no.difi.commons.asic.jaxb.asic.Certificate;

import java.io.*;
import java.nio.file.Path;
import java.util.List;

public interface AsicReader extends Closeable {

    /**
     * Provides the name of the next entry in the ASiC archive and positions the inputstream at the beginning of the data.
     *
     * @return name of next entry in archive.
     * @throws IOException
     */
    String getNextFile() throws IOException;

    /**
     * Writes the contents of the current entry into a file
     *
     * @param file into which the contents should be written.
     * @throws IOException
     */
    void writeFile(File file) throws IOException;

    /**
     * Writes contents of current archive entry into a file.
     *
     * @param path into which the contents of current entry should be written.
     * @throws IOException
     */
    void writeFile(Path path) throws IOException;

    /**
     * Writes contents of current archive entry to the supplied output stream.
     *
     * @param outputStream into which data from current entry should be written.
     * @throws IOException
     */
    void writeFile(OutputStream outputStream) throws IOException;

    /**
     * Returns InputStream to read the content.
     *
     * @return Content
     */
    InputStream inputStream() throws IOException;

    AsicManifest getAsicManifest();

    List<Certificate> getCertificateChain();
}
