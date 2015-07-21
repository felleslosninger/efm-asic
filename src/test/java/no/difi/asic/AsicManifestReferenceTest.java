package no.difi.asic;

import org.etsi.uri._2918.v1_2.ASiCManifestType;
import org.etsi.uri._2918.v1_2.DataObjectReferenceType;
import org.etsi.uri._2918.v1_2.ObjectFactory;
import org.etsi.uri._2918.v1_2.SigReferenceType;
import org.testng.annotations.Test;
import org.w3._2000._09.xmldsig_.DigestMethodType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

/**
 * @author steinar
 *         Date: 03.07.15
 *         Time: 09.09
 */
public class AsicManifestReferenceTest {

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
            digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");
            obj1.setDigestMethod(digestMethodType);
            obj1.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReference().add(obj1);
        }

        {
            DataObjectReferenceType obj2 = new DataObjectReferenceType();
            obj2.setURI("bii-document.xml");
            obj2.setMimeType("application/xml");

            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha256");
            obj2.setDigestMethod(digestMethodType);
            obj2.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReference().add(obj2);
        }


        JAXBContext jaxbContext = JAXBContext.newInstance(ASiCManifestType.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // The ASiCManifestType is not annotated
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<ASiCManifestType> m = objectFactory.createASiCManifest(asicManifest);


        marshaller.marshal(m, System.out);
    }


}
