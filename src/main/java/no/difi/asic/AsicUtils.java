package no.difi.asic;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.zip.ZipEntry;

public class AsicUtils {

    /** The MIME type, which should be the very first entry in the container */
    public static final String MIMETYPE_ASICE = "application/vnd.etsi.asic-e+zip";

    AsicUtils() {
        // No action
    }

    /**
     * Combine multiple containers to one container.
     *
     * Does not preserve META-INF/manifest.xml.
     *
     * @param outputStream Stream for target container.
     * @param inputStreams Streams for source containers.
     */
    public static void combine(OutputStream outputStream, InputStream... inputStreams) throws IOException {
        AsicOutputStream target = new AsicOutputStream(outputStream);
        int counter = 0;
        boolean containsRootFile = false;

        for (InputStream inputStream : inputStreams) {
            AsicInputStream source = new AsicInputStream(inputStream);

            ZipEntry zipEntry;
            while ((zipEntry = source.getNextEntry()) != null) {
                // TODO Better code to make sure manifest filenames doesn't collide.
                if (zipEntry.getName().startsWith("META-INF/asicmanifest")) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(source, byteArrayOutputStream);

                    // Read manifest to make sure only on rootfile makes it to the source container.
                    ManifestVerifier manifestVerifier = new ManifestVerifier(null);
                    CadesAsicManifest.extractAndVerify(byteArrayOutputStream.toString(), manifestVerifier);

                    String rootFile = manifestVerifier.getAsicManifest().getRootfile();
                    if (rootFile != null) {
                        if (containsRootFile)
                            throw new IllegalStateException("Multiple rootfiles is not allowed when combining containers.");
                        containsRootFile = true;
                    }

                    target.putNextEntry(new ZipEntry(String.format("META-INF/asicmanifest%s.xml", ++counter)));
                    IOUtils.copy(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), target);
                } else if (zipEntry.getName().equals("META-INF/manifest.xml")) {
                    continue;
                } else {
                    target.putNextEntry(zipEntry);
                    IOUtils.copy(source, target);
                }

                source.closeEntry();
                target.closeEntry();
            }

            source.close();
        }

        target.close();
    }

}