package no.difi.asic;

import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.etsi.uri._2918.v1_1.DataObjectReferenceType;
import org.etsi.uri._2918.v1_1.ObjectFactory;
import org.w3._2000._09.xmldsig_.DigestMethodType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.Collection;

/**
 * Holds the object graph representing the ASiCManifest, i.e. the list of data objects (files)
 * added, together with their associated attributes like checksums and URIs
 *
 * @author steinar
 *         Date: 05.07.15
 *         Time: 17.21
 */
public class AsicManifestReference {


    private final ASiCManifestType ASiCManifestType;

    AsicManifestReference(ASiCManifestType ASiCManifestType) {
        this.ASiCManifestType = ASiCManifestType;
    }

    public AsicManifestReference(Collection<AsicDataObjectEntry> entries) {

        ASiCManifestType = new ASiCManifestType();

        for (AsicDataObjectEntry entry : entries) {
            DataObjectReferenceType dataObjectReferenceType = new DataObjectReferenceType();
            dataObjectReferenceType.setURI(entry.getUri().toASCIIString());
            dataObjectReferenceType.setMimeType(entry.getMimeType().toString());

            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");

            dataObjectReferenceType.setDigestMethod(digestMethodType);
            dataObjectReferenceType.setDigestValue(entry.getDigestBytes());


            ASiCManifestType.getDataObjectReference().add(dataObjectReferenceType);
        }
    }

    public byte[] toBytes(JAXBContext jaxbContext) {

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ObjectFactory objectFactory = new ObjectFactory();

            JAXBElement<ASiCManifestType> jaxbRootElement = objectFactory.createASiCManifest(ASiCManifestType);


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(jaxbRootElement, baos);
            byte[] bytes = baos.toByteArray();
            return bytes;
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the ASiCManifest into string output", e);
        }
    }
}
