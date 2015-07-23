package no.difi.asic;

import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._2918.v1_2.ASiCManifest;
import org.etsi.uri._2918.v1_2.DataObjectReference;
import org.etsi.uri._2918.v1_2.SigReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2000._09.xmldsig_.DigestMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

class CadesAsicManifest extends AbstractAsicManifest {

    public static final Logger log = LoggerFactory.getLogger(AbstractAsicManifest.class);

    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ASiCManifest.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    // Automagically generated from XML Schema Definition files
    private ASiCManifest ASiCManifestType = new ASiCManifest();

    public CadesAsicManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        super(messageDigestAlgorithm);
    }

    @Override
    public void add(String filename, MimeType mimeType) {
        DataObjectReference dataObject = new DataObjectReference();
        dataObject.setURI(filename);
        dataObject.setMimeType(mimeType.toString());
        dataObject.setDigestValue(messageDigest.digest());

        DigestMethod digestMethodType = new DigestMethod();
        digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
        dataObject.setDigestMethod(digestMethodType);

        ASiCManifestType.getDataObjectReferences().add(dataObject);
        log.debug(String.format("Digest: %s", new String(Base64.encode(dataObject.getDigestValue()))));
    }

    public void setSignature(String filename, String mimeType) {
        SigReference sigReferenceType = new SigReference();
        sigReferenceType.setURI(filename);
        sigReferenceType.setMimeType(mimeType);
        ASiCManifestType.setSigReference(sigReferenceType);
    }

    public ASiCManifest getASiCManifestType() {
        return ASiCManifestType;
    }

    public byte[] toBytes() {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(ASiCManifestType, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the ASiCManifest into string output", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void extractAndVerify(InputStream inputStream, ManifestVerifier manifestVerifier) {
        try {
            // Read XML
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ASiCManifest manifest = (ASiCManifest) unmarshaller.unmarshal(inputStream);

            String sigReference = manifest.getSigReference().getURI();
            if (sigReference == null)
                sigReference = "META-INF/signature.p7s";

            // Run through recorded objects
            for (DataObjectReference reference : manifest.getDataObjectReferences())
                manifestVerifier.update(reference.getURI(), reference.getMimeType(), reference.getDigestValue(), reference.getDigestMethod().getAlgorithm(), sigReference);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to read content as XML");
        }
    }

}
