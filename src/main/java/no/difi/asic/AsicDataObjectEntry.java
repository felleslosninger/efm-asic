package no.difi.asic;

import javax.activation.MimeType;
import java.io.File;
import java.net.URI;

/**
 * Holds the information we need to obtain or compute for each data object entry (file) in the
 * ASiC container.
 *
 * @author steinar
 *         Date: 11.07.15
 *         Time: 18.21
 */
public class AsicDataObjectEntry {

    private String  name;
    private File    file;
    private MimeType mimeType;
    private URI     uri;
    private byte[] digestBytes;


    public AsicDataObjectEntry(String entryName, File fileReference, MimeType mimeType, URI uri) {

        name = entryName;
        file = fileReference;
        this.mimeType = mimeType;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public byte[] getDigestBytes() {
        return digestBytes;
    }

    public void setDigestBytes(byte[] digestBytes) {
        this.digestBytes = digestBytes;
    }
}
