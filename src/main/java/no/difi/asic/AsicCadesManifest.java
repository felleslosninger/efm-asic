package no.difi.asic;

import com.sun.xml.bind.api.JAXBRIContext;
import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._2918.v1_2.ASiCManifestType;
import org.etsi.uri._2918.v1_2.DataObjectReferenceType;
import org.etsi.uri._2918.v1_2.ObjectFactory;
import org.etsi.uri._2918.v1_2.SigReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2000._09.xmldsig_.DigestMethodType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;

class AsicCadesManifest extends AsicAbstractManifest {

    public static final Logger log = LoggerFactory.getLogger(AsicAbstractManifest.class);

    private static ObjectFactory objectFactory = new ObjectFactory();
    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBRIContext.newInstance(ASiCManifestType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    // Automagically generated from XML Schema Definition files
    private ASiCManifestType ASiCManifestType = new ASiCManifestType();

    public AsicCadesManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        super(messageDigestAlgorithm);
    }

    @Override
    public void add(String filename, String mimeType) {
        DataObjectReferenceType dataObject = new DataObjectReferenceType();
        dataObject.setURI(filename);
        dataObject.setMimeType(mimeType);
        dataObject.setDigestValue(messageDigest.digest());

        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
        dataObject.setDigestMethod(digestMethodType);

        ASiCManifestType.getDataObjectReference().add(dataObject);
        log.debug(String.format("Digest: %s", new String(Base64.encode(dataObject.getDigestValue()))));
    }

    public void setSignature(String filename, String mimeType) {
        SigReferenceType sigReferenceType = new SigReferenceType();
        sigReferenceType.setURI(filename);
        sigReferenceType.setMimeType(mimeType);
        ASiCManifestType.setSigReference(sigReferenceType);
    }

    public ASiCManifestType getASiCManifestType() {
        return ASiCManifestType;
    }

    public byte[] toBytes() {
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
