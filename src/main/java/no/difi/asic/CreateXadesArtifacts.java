package no.difi.asic;

import com.google.common.hash.Hashing;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import no.difi.commons.asic.jaxb.xades.*;
import no.difi.commons.asic.jaxb.xmldsig.DigestMethodType;
import no.difi.commons.asic.jaxb.xmldsig.X509IssuerSerialType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.dom.DOMResult;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.List;

import static java.util.stream.IntStream.range;

public class CreateXadesArtifacts {

    private static final JAXBContext jaxbContext; // Thread safe
    private static final DigestMethodType digestMethod;

    static {
        try {
            jaxbContext = JAXBContext.newInstance(QualifyingPropertiesType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }

        DigestMethodType type = new DigestMethodType();
        type.getContent();
        type.setAlgorithm(javax.xml.crypto.dsig.DigestMethod.SHA1);
        digestMethod = type;
    }

    XadesArtifacts createArtifactsToSign(List<DataObjectFormatType> dataObjectFormatList, X509Certificate certificate) {
        byte[] certificateDigestValue = getCertificateDigestValue(certificate);

        DigestAlgAndValueType certificateDigest = new DigestAlgAndValueType();
        certificateDigest.setDigestMethod(digestMethod);
        certificateDigest.setDigestValue(certificateDigestValue);

        X509IssuerSerialType certificateIssuer = new X509IssuerSerialType();
        certificateIssuer.setX509IssuerName(certificate.getIssuerDN().getName());
        certificateIssuer.setX509SerialNumber(certificate.getSerialNumber());

        CertIDType certID = new CertIDType();
        certID.setCertDigest(certificateDigest);
        certID.setIssuerSerial(certificateIssuer);

        CertIDListType signingCertificate = new CertIDListType();
        signingCertificate.getCert().add(certID);

        SignedSignaturePropertiesType signedSignatureProperties = new SignedSignaturePropertiesType();
        signedSignatureProperties.setSigningTime(getSigningTime());
        signedSignatureProperties.setSigningCertificate(signingCertificate);

        SignedDataObjectPropertiesType signedDataObjectProperties = new SignedDataObjectPropertiesType();
        signedDataObjectProperties.getDataObjectFormat().addAll(dataObjectFormatList);

        SignedPropertiesType signedProperties = new SignedPropertiesType();
        signedProperties.setSignedSignatureProperties(signedSignatureProperties);
        signedProperties.setSignedDataObjectProperties(signedDataObjectProperties);
        signedProperties.setId("SignedProperties");

        QualifyingPropertiesType qualifyingProperties = new QualifyingPropertiesType();
        qualifyingProperties.setSignedProperties(signedProperties);
        qualifyingProperties.setTarget("#Signature");

        return from(qualifyingProperties);
    }

    private byte[] getCertificateDigestValue(X509Certificate certificate) {
        try {
            return sha1(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("getCertificateDigestValue failed!", e);
        }
    }

    private byte[] sha1(byte[] in) {
        return Hashing.sha1().hashBytes(in).asBytes();
    }

    private XMLGregorianCalendar getSigningTime() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Could not get signing time", e);
        }
    }

    private XadesArtifacts from(QualifyingPropertiesType qualifyingProperties) {
        DOMResult domResult = new DOMResult();
        try {
            jaxbContext.createMarshaller().marshal(new ObjectFactory().createQualifyingProperties(qualifyingProperties), domResult);
        } catch (JAXBException e) {
            throw new IllegalStateException("Could not marshal QualifyingProperties", e);
        }
        return from((Document) domResult.getNode());
    }

    private XadesArtifacts from(Document qualifyingPropertiesDocument) {
        Element qualifyingProperties = qualifyingPropertiesDocument.getDocumentElement();
        NodeList qualifyingPropertiesContents = qualifyingProperties.getChildNodes();
        Element signedProperties = range(0, qualifyingPropertiesContents.getLength()).mapToObj(qualifyingPropertiesContents::item)
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(Element.class::cast)
                .filter(element -> "SignedProperties".equals(element.getLocalName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Didn't find SignedProperties in document."));
        String signerPropertiesReferenceUri = signedProperties.getAttribute("Id");
        return new XadesArtifacts(qualifyingPropertiesDocument, signedProperties, "#" + signerPropertiesReferenceUri);
    }
}