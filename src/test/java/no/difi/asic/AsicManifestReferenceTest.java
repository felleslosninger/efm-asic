package no.difi.asic;

import org.etsi.uri._02918.v1_2.ASiCManifest;
import org.etsi.uri._02918.v1_2.DataObjectReference;
import org.etsi.uri._02918.v1_2.ObjectFactory;
import org.etsi.uri._02918.v1_2.SigReference;
import org.testng.annotations.Test;
import org.w3._2000._09.xmldsig_.DigestMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 * @author steinar
 *         Date: 03.07.15
 *         Time: 09.09
 */
public class AsicManifestReferenceTest {

    @Test
    public void createSampleManifest() throws Exception {

        ASiCManifest asicManifest = new ASiCManifest();

        SigReference sigReferenceType = new SigReference();
        sigReferenceType.setURI("META-INF/signature.p7s");             // TODO: implement signature
        sigReferenceType.setMimeType("application/x-pkcs7-signature");  // TODO: use strong typed Mime types
        asicManifest.setSigReference(sigReferenceType);

        {
            DataObjectReference obj1 = new DataObjectReference();
            obj1.setURI("bii-envelope.xml");            // TODO: retrieve doc name from container
            obj1.setMimeType("application/xml");

            DigestMethod digestMethodType = new DigestMethod();
            digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
            obj1.setDigestMethod(digestMethodType);
            obj1.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReferences().add(obj1);
        }

        {
            DataObjectReference obj2 = new DataObjectReference();
            obj2.setURI("bii-document.xml");
            obj2.setMimeType("application/xml");

            DigestMethod digestMethodType = new DigestMethod();
            digestMethodType.setAlgorithm("http://www.w3.org/2001/04/xmlenc#sha256");
            obj2.setDigestMethod(digestMethodType);
            obj2.setDigestValue("j61wx3SAvKTMUP4NbeZ1".getBytes());

            asicManifest.getDataObjectReferences().add(obj2);
        }


        JAXBContext jaxbContext = JAXBContext.newInstance(ASiCManifest.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // The ASiCManifestType is not annotated
        ObjectFactory objectFactory = new ObjectFactory();
        // JAXBElement<ASiCManifestType> m = objectFactory.createASiCManifest(asicManifest);


        // marshaller.marshal(m, System.out);
        marshaller.marshal(asicManifest, System.out);
    }


}
