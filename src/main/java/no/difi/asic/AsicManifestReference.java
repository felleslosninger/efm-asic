package no.difi.asic;

import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.etsi.uri._2918.v1_1.ObjectFactory;

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

    private static final ObjectFactory objectFactory = new ObjectFactory();

    private final ASiCManifestType ASiCManifestType;

    public AsicManifestReference(Collection<AsicDataObjectEntry> entries) {

        this.ASiCManifestType = objectFactory.createASiCManifestType();

        for (AsicDataObjectEntry entry : entries)
            ASiCManifestType.getDataObjectReference().add(entry.getDataObject());
    }

    public byte[] toBytes(JAXBContext jaxbContext) {

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            JAXBElement<ASiCManifestType> jaxbRootElement = objectFactory.createASiCManifest(ASiCManifestType);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(jaxbRootElement, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the ASiCManifest into string output", e);
        }
    }
}
