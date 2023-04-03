package no.difi.asic;

import jakarta.xml.bind.*;
import no.difi.commons.asic.jaxb.cades.ASiCManifestType;
import no.difi.commons.asic.jaxb.cades.DataObjectReferenceType;
import no.difi.commons.asic.jaxb.cades.ObjectFactory;
import no.difi.commons.asic.jaxb.cades.SigReferenceType;
import no.difi.commons.asic.jaxb.xmldsig.DigestMethodType;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

class CadesAsicManifest extends AbstractAsicManifest {

    public static final Logger logger = LoggerFactory.getLogger(AbstractAsicManifest.class);

    private static JAXBContext jaxbContext; // Thread safe
    private static ObjectFactory objectFactory = new ObjectFactory();

    static {
        try {
            jaxbContext = JAXBContext.newInstance(ASiCManifestType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    // Automagically generated from XML Schema Definition files
    private ASiCManifestType ASiCManifestType = new ASiCManifestType();
    private boolean rootFilenameIsSet = false;

    public CadesAsicManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        super(messageDigestAlgorithm);
    }

    @Override
    public void add(String filename, MimeType mimeType) {
        DataObjectReferenceType dataObject = new DataObjectReferenceType();
        dataObject.setURI(filename);
        dataObject.setMimeType(mimeType.toString());
        dataObject.setDigestValue(messageDigest.digest());

        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
        dataObject.setDigestMethod(digestMethodType);

        ASiCManifestType.getDataObjectReference().add(dataObject);
        logger.debug("Digest: {}", Base64.encode(dataObject.getDigestValue()));
    }

    /**
     * Locates the DataObjectReference for the given file name and sets the attribute Rootfile to Boolean.TRUE
     *
     * @param entryName name of entry for which the attribute <code>Rootfile</code> should be set to "true".
     */
    public void setRootfileForEntry(String entryName) {
        if (rootFilenameIsSet)
            throw new IllegalStateException("Multiple root files are not allowed.");

        for (DataObjectReferenceType dataObject : ASiCManifestType.getDataObjectReference()) {
            if (dataObject.getURI().equals(entryName)) {
                dataObject.setRootfile(true);
                rootFilenameIsSet = true;
                return;
            }
        }
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(objectFactory.createASiCManifest(ASiCManifestType), baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the ASiCManifest into string output", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static String extractAndVerify(String xml, ManifestVerifier manifestVerifier) {
        // Updating namespaces for compatibility with previous releases and other implementations

        xml = xml.replace("http://uri.etsi.org/02918/v1.1.1#", "http://uri.etsi.org/02918/v1.2.1#");
        xml = xml.replace("http://uri.etsi.org/2918/v1.2.1#", "http://uri.etsi.org/02918/v1.2.1#");
        xml = xml.replaceAll("http://www.w3.org/2000/09/xmldsig#sha", "http://www.w3.org/2001/04/xmlenc#sha");

        try {
            // Read XML
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            ASiCManifestType manifest = (ASiCManifestType) ((JAXBElement) unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()))).getValue();

            String sigReference = manifest.getSigReference().getURI();
            if (sigReference == null)
                sigReference = "META-INF/signature.p7s";

            // Run through recorded objects
            for (DataObjectReferenceType reference : manifest.getDataObjectReference()) {
                manifestVerifier.update(reference.getURI(), reference.getMimeType(), reference.getDigestValue(), reference.getDigestMethod().getAlgorithm(), sigReference);
                if (reference.isRootfile() == Boolean.TRUE)
                    manifestVerifier.setRootFilename(reference.getURI());
            }

            return sigReference;
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to read content as XML", e);
        }
    }

}
