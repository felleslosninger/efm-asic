package no.difi.asic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.09
 */
public class AsicBuilder {

    // Holds the list of files to be added
    private Map<String, File> files = new HashMap<String, File>();

    /**
     * Holds the archiveName of container, which may not contain special characters like for instance ':', '/', '\\'
     */
    private String archiveName;
    private File outputDir;

    /**
     * Adds the supplied file into the archive, using the complete path unless it is absolute in which
     * case it is added to the archive under the basic file archiveName (entire path removed).
     *
     * @param file reference to file to be added.
     */
    public AsicBuilder addFile(File file) {
        if (file.isAbsolute()) {
            addFile(file, file.getName());
        } else {
            addFile(file, file.toString());
        }

        return this;
    }

    public void addFile(File fileReference, String entryName) {
        files.put(entryName, fileReference);
    }


    public AsicBuilder archiveName(String name) {
        this.archiveName = name;

        // TODO: verify there are no special characters in the archiveName

        return this;
    }

    public AsicContainer build() {
        return new AsicContainer(this);
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public String getArchiveName() {
        return archiveName;
    }

    public File getOutputDir() {
        return outputDir;
    }

    public AsicBuilder outputDirectory(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

}
