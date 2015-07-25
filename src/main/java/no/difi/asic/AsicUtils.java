package no.difi.asic;

import oasis.names.tc.opendocument.xmlns.manifest._1.FileEntry;
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
        // Statuses
        int manifestCounter = 0;
        int fileCounter = 0;
        boolean containsRootFile = false;

        // Open target container
        AsicOutputStream target = new AsicOutputStream(outputStream);

        // Prepare to combine OASIS OpenDocument Manifests
        OasisManifest oasisManifest = new OasisManifest(MimeType.forString(MIMETYPE_ASICE));

        for (InputStream inputStream : inputStreams) {
            // Open source container
            AsicInputStream source = new AsicInputStream(inputStream);

            // Read entries
            ZipEntry zipEntry;
            while ((zipEntry = source.getNextEntry()) != null) {
                // TODO Better code to make sure XAdES manifest filenames doesn't collide.
                if (zipEntry.getName().startsWith("META-INF/asicmanifest")) {
                    // Fetch content
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(source, byteArrayOutputStream);

                    // Read manifest
                    ManifestVerifier manifestVerifier = new ManifestVerifier(null);
                    CadesAsicManifest.extractAndVerify(byteArrayOutputStream.toString(), manifestVerifier);

                    // Make sure only on rootfile makes it to the source container
                    if (manifestVerifier.getAsicManifest().getRootfile() != null) {
                        if (containsRootFile)
                            throw new IllegalStateException("Multiple rootfiles is not allowed when combining containers.");
                        containsRootFile = true;
                    }

                    // Write manifest to container
                    target.putNextEntry(new ZipEntry(String.format("META-INF/asicmanifest%s.xml", ++manifestCounter)));
                    IOUtils.copy(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), target);
                } else if (zipEntry.getName().equals("META-INF/manifest.xml")) {
                    // Fetch content
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    IOUtils.copy(source, byteArrayOutputStream);

                    // Copy entries
                    OasisManifest sourceOasisManifest = new OasisManifest(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                    for (FileEntry fileEntry : sourceOasisManifest.getManifest().getFileEntries())
                        if (!fileEntry.getFullPath().equals("/"))
                            oasisManifest.add(fileEntry);

                    // Nothing to write to target container
                    target.closeEntry();
                    continue;
                } else {
                    // Copy content to target container
                    target.putNextEntry(zipEntry);
                    IOUtils.copy(source, target);

                    if (!zipEntry.getName().startsWith("META-INF/"))
                        fileCounter++;
                }

                source.closeEntry();
                target.closeEntry();
            }

            // Close source container
            source.close();
        }

        // Add manifest if it contains the same amount of files as the container.
        if (oasisManifest.size() == fileCounter + 1)
            target.writeZipEntry("META-INF/manifest.xml", oasisManifest.toBytes());

        // Close target container
        target.close();
    }
}