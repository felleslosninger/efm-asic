package no.difi.asic;

import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.etsi.uri._2918.v1_1.DataObjectReferenceType;
import org.w3._2000._09.xmldsig_.DigestMethodType;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author steinar
 *         Date: 05.07.15
 *         Time: 17.21
 */
public class AsicManifestReference {


    private final ASiCManifestType aSiCManifestType;

    public AsicManifestReference(ASiCManifestType aSiCManifestType, Builder builder) {
        this.aSiCManifestType = aSiCManifestType;
    }

    public ASiCManifestType getaSiCManifestType() {
        return aSiCManifestType;
    }

    public static class Builder {

        List<DataObjectReference> dataObjects = new ArrayList<>();

        public void addDataObjectReference(URI uri, String mimeType, byte[] digestBytes) {
            dataObjects.add(new DataObjectReference(uri, mimeType, digestBytes));
        }

        public AsicManifestReference build() {

            ASiCManifestType aSiCManifestType = new ASiCManifestType();

            for (DataObjectReference dataObject : dataObjects) {
                DataObjectReferenceType dataObjectReferenceType = new DataObjectReferenceType();
                dataObjectReferenceType.setURI(dataObject.getUri().toASCIIString());
                dataObjectReferenceType.setMimeType(dataObject.getMimeType());

                DigestMethodType digestMethodType = new DigestMethodType();
                digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");

                dataObjectReferenceType.setDigestMethod(digestMethodType);
                dataObjectReferenceType.setDigestValue(dataObject.getDigestBytes());
                aSiCManifestType.getDataObjectReference().add(dataObjectReferenceType);
            }

            return new AsicManifestReference(aSiCManifestType, this);
        }
    }

    static class DataObjectReference {

        private final URI uri;
        private final String mimeType;
        private final byte[] digestBytes;

        public DataObjectReference(URI uri, String mimeType, byte[] digestBytes) {

            this.uri = uri;
            this.mimeType = mimeType;
            this.digestBytes = digestBytes;
        }

        public URI getUri() {
            return uri;
        }

        public String getMimeType() {
            return mimeType;
        }

        public byte[] getDigestBytes() {
            return digestBytes;
        }
    }

}
