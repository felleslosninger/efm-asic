package no.difi.asic;

import org.etsi.uri._2918.v1_1.DataObjectReferenceType;
import org.w3._2000._09.xmldsig_.DigestMethodType;

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

    private DataObjectReferenceType dataObject = new DataObjectReferenceType();

    public AsicDataObjectEntry(String entryName, File fileReference, MimeType mimeType, URI uri) {
        this.name = entryName;
        this.file = fileReference;

        this.dataObject.setURI(uri.toASCIIString());
        this.dataObject.setMimeType(mimeType.toString());

        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");
        this.dataObject.setDigestMethod(digestMethodType);
    }

    public String getName() {
        return this.name;
    }

    public File getFile() {
        return this.file;
    }

    void setDigestBytes(byte[] digestBytes) {
        this.dataObject.setDigestValue(digestBytes);
    }

    DataObjectReferenceType getDataObject() {
        return this.dataObject;
    }
}
