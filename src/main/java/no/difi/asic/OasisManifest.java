package no.difi.asic;

import oasis.names.tc.opendocument.xmlns.manifest._1.Manifest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

public class OasisManifest {

    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBContext.newInstance(Manifest.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    static Manifest read(InputStream inputStream) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Manifest) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to read XML as OASIS OpenDocument Manifest.");
        }
    }
}
