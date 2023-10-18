package no.difi.asic;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import no.difi.commons.asic.jaxb.cades.XAdESSignaturesType;
import no.difi.commons.asic.jaxb.xades.DataObjectFormatType;
import no.difi.commons.asic.jaxb.xades.QualifyingPropertiesType;
import no.difi.commons.asic.jaxb.xades.SignedDataObjectPropertiesType;
import no.difi.commons.asic.jaxb.xmldsig.ReferenceType;
import no.difi.commons.asic.jaxb.xmldsig.SignatureType;
import no.difi.commons.asic.jaxb.xmldsig.SignedInfoType;
import no.difi.commons.asic.jaxb.xmldsig.X509DataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

class XadesAsicManifest extends AbstractAsicManifest {

    private static final String C14V1 = CanonicalizationMethod.INCLUSIVE;
    private static final String ASIC_NAMESPACE = "http://uri.etsi.org/02918/v1.2.1#";
    private static final String SIGNED_PROPERTIES_TYPE = "http://uri.etsi.org/01903#SignedProperties";

    private static final JAXBContext jaxbContext; // Thread safe
    private static final CreateXadesArtifacts createXAdESArtifacts = new CreateXadesArtifacts();

    static {
        try {
            jaxbContext = JAXBContext.newInstance(XAdESSignaturesType.class, X509DataType.class, QualifyingPropertiesType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    private final XMLSignatureFactory xmlSignatureFactory;
    private final SignatureMethod signatureMethod;
    private final CanonicalizationMethod canonicalizationMethod;
    private final Transform canonicalXmlTransform;
    private final DigestMethod digestMethod;
    private final List<Reference> references = new ArrayList<>();
    // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties
    private final SignedDataObjectPropertiesType signedDataObjectProperties = new SignedDataObjectPropertiesType();

    public XadesAsicManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        super(messageDigestAlgorithm);

        xmlSignatureFactory = getSignatureFactory();
        signatureMethod = getSignatureMethod(xmlSignatureFactory);

        try {
            canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(C14V1, (C14NMethodParameterSpec) null);
            canonicalXmlTransform = xmlSignatureFactory.newTransform(C14V1, (TransformParameterSpec) null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Kunne ikke initialisere xml-signering", e);
        }

        digestMethod = getDigestMethod(messageDigestAlgorithm);
    }

    private XMLSignatureFactory getSignatureFactory() {
        try {
            return XMLSignatureFactory.getInstance("DOM", "XMLDSig");
        } catch (NoSuchProviderException e) {
            throw new IllegalStateException("Could not find provider for DOM:XMLDSig", e);
        }
    }

    private SignatureMethod getSignatureMethod(XMLSignatureFactory xmlSignatureFactory) {
        try {
            return xmlSignatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Could not get signature method", e);
        }
    }

    private DigestMethod getDigestMethod(MessageDigestAlgorithm messageDigestAlgorithm) {
        try {
            return xmlSignatureFactory.newDigestMethod(messageDigestAlgorithm.getUri(), null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Could not create DigestMethod!", e);
        }
    }

    @Override
    public void add(String filename, MimeType mimeType) {
        String id = String.format("ID_%s", references.size());

        references.add(xmlSignatureFactory.newReference(
                encodeFilename(filename),
                digestMethod,
                null,
                null,
                id, messageDigest.digest()));

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties\DataObjectFormat
        DataObjectFormatType dataObjectFormatType = new DataObjectFormatType();
        dataObjectFormatType.setObjectReference(String.format("#%s", id));
        dataObjectFormatType.setMimeType(mimeType.toString());

        signedDataObjectProperties.getDataObjectFormat().add(dataObjectFormatType);
    }

    private String encodeFilename(String filename) {
        try {
            return URLEncoder.encode(filename, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(String.format("Could not URL encode filename = '%s'", filename), e);
        }
    }

    public byte[] toBytes(SignatureHelper signatureHelper) {
        // Generer XAdES-dokument som skal signeres, informasjon om nøkkel brukt til signering og informasjon om hva som er signert
        XadesArtifacts xadesArtifacts = createXAdESArtifacts.createArtifactsToSign(
                signedDataObjectProperties.getDataObjectFormat(),
                signatureHelper.getX509Certificate());

        // Lag signatur-referanse for XaDES properties
        references.add(xmlSignatureFactory.newReference(
                xadesArtifacts.getSignablePropertiesReferenceUri(),
                digestMethod,
                singletonList(canonicalXmlTransform),
                SIGNED_PROPERTIES_TYPE,
                null
        ));

        KeyInfo keyInfo = keyInfo(signatureHelper.getCertificateChain());
        // \XAdESSignature\Signature\SignedInfo
        SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, references);

        // Definer signatur over XAdES-dokument
        XMLObject xmlObject = xmlSignatureFactory.newXMLObject(singletonList(new DOMStructure(xadesArtifacts.getDocument().getDocumentElement())), null, null, null);
        XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, keyInfo, singletonList(xmlObject), "Signature", null);

        Document signedDocument = DomUtils.newEmptyXmlDocument();
        DOMSignContext signContext = new DOMSignContext(signatureHelper.keyPair.getPrivate(), addXAdESSignaturesElement(signedDocument));
        signContext.setURIDereferencer(signedPropertiesURIDereferencer(xadesArtifacts));

        try {
            xmlSignature.sign(signContext);
        } catch (MarshalException e) {
            throw new IllegalStateException("Could not marshal ASiC-E signature.xml", e);
        } catch (XMLSignatureException e) {
            throw new IllegalStateException("Could not sign ASiC-E", e);
        }

        return DomUtils.serializeToXml(signedDocument);
    }

    public List<Reference> getReferences() {
        return Collections.unmodifiableList(references);
    }

    private URIDereferencer signedPropertiesURIDereferencer(XadesArtifacts xadesArtifacts) {
        return (uriReference, context) -> {
            if (xadesArtifacts.getSignablePropertiesReferenceUri().equals(uriReference.getURI())) {
                return (NodeSetData) DomUtils.allNodesBelow(xadesArtifacts.getSignableProperties())::iterator;
            }
            return xmlSignatureFactory.getURIDereferencer().dereference(uriReference, context);
        };
    }

    private static org.w3c.dom.Element addXAdESSignaturesElement(Document doc) {
        return (Element) doc.appendChild(doc.createElementNS(ASIC_NAMESPACE, "XAdESSignatures"));
    }

    private KeyInfo keyInfo(final Certificate[] sertifikater) {
        KeyInfoFactory keyInfoFactory = xmlSignatureFactory.getKeyInfoFactory();
        X509Data x509Data = keyInfoFactory.newX509Data(asList(sertifikater));
        return keyInfoFactory.newKeyInfo(singletonList(x509Data));
    }

    public static void extractAndVerify(String xml, ManifestVerifier manifestVerifier) {
        // Updating namespace
        xml = xml.replace("http://uri.etsi.org/02918/v1.1.1#", ASIC_NAMESPACE)
                .replace("http://uri.etsi.org/2918/v1.2.1#", ASIC_NAMESPACE)
                .replaceAll("http://www.w3.org/2000/09/xmldsig#sha", "http://www.w3.org/2001/04/xmlenc#sha");

        XAdESSignaturesType manifest;

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            manifest = unmarshaller.unmarshal(new StreamSource(new ByteArrayInputStream(xml.getBytes())), XAdESSignaturesType.class).getValue();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read content as XML", e);
        }

        for (SignatureType signature : manifest.getSignature()) {
            SignedInfoType signedInfoType = signature.getSignedInfo();

            for (ReferenceType reference : signedInfoType.getReference()) {
                if (!reference.getURI().startsWith("#"))
                    manifestVerifier.update(reference.getURI(), null, reference.getDigestValue(), reference.getDigestMethod().getAlgorithm(), null);
            }
        }
    }
}
