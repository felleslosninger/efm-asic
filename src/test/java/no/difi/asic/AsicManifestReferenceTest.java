package no.difi.asic;

import no.difi.commons.asic.jaxb.cades.*;
import no.difi.commons.asic.jaxb.xmldsig.DigestMethodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

/**
 * @author steinar
 *         Date: 03.07.15
 *         Time: 09.09
 */
public class AsicManifestReferenceTest {

    private static Logger log = LoggerFactory.getLogger(AsicManifestReferenceTest.class);

    @Test
    public void createSampleManifest() throws Exception {

        ASiCManifestType asicManifest = new ASiCManifestType();

        SigReferenceType sigReferenceType = new SigReferenceType();
        sigReferenceType.setURI("META-INF/signature.p7s");             // TODO: implement signature
        sigReferenceType.setMimeType("application/x-pkcs7-signature");  // TODO: use strong typed Mime types
        asicManifest.setSigReference(sigReferenceType);

        {
            DataObjectReferenceType obj1 = new DataObjectReferenceType();
            obj1.setURI("bii-envelope.xml");            // TODO: retrieve doc name from container
            obj1.setMimeType("application/xml");

            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
            obj1.setDigestMethod(digestMethodType);
            obj1.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReference().add(obj1);
        }

        {
            DataObjectReferenceType obj2 = new DataObjectReferenceType();
            obj2.setURI("bii-document.xml");
            obj2.setMimeType("application/xml");

            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
            obj2.setDigestMethod(digestMethodType);
            obj2.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReference().add(obj2);
        }


        JAXBContext jaxbContext = JAXBContext.newInstance(ASiCManifestType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // The ASiCManifestType is not annotated
        ObjectFactory objectFactory = new ObjectFactory();
        // JAXBElement<ASiCManifestType> m = objectFactory.createASiCManifest(asicManifest);


        // marshaller.marshal(m, System.out);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        marshaller.marshal(objectFactory.createASiCManifest(asicManifest), byteArrayOutputStream);
        log.info(byteArrayOutputStream.toString());
    }


}
