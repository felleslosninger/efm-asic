package no.difi.asic;

import com.sun.xml.bind.api.JAXBRIContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.etsi.uri._01903.v1_3.*;
import org.etsi.uri._2918.v1_2.ObjectFactory;
import org.etsi.uri._2918.v1_2.XAdESSignaturesType;
import org.w3._2000._09.xmldsig_.*;
import sun.security.x509.X509CertImpl;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.ByteArrayOutputStream;
import java.security.cert.CertificateEncodingException;
import java.util.GregorianCalendar;

class AsicXadesManifest extends AsicAbstractManifest {

    private static ObjectFactory objectFactory = new ObjectFactory();
    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBRIContext.newInstance(XAdESSignaturesType.class, X509DataType.class, QualifyingPropertiesType.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    // \XAdESSignature\Signature\SignedInfo
    private SignedInfoType signedInfo;
    // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties
    private SignedDataObjectPropertiesType signedDataObjectProperties = new SignedDataObjectPropertiesType();

    public AsicXadesManifest() {
        super(MessageDigestAlgorithm.SHA256);

        // \XAdESSignature\Signature\SignedInfo
        signedInfo = new SignedInfoType();

        // \XAdESSignature\Signature\SignedInfo\CanonicalizationMethod
        CanonicalizationMethodType canonicalizationMethod = new CanonicalizationMethodType();
        canonicalizationMethod.setAlgorithm("http://www.w3.org/2006/12/xml-c14n11");
        signedInfo.setCanonicalizationMethod(canonicalizationMethod);

        // \XAdESSignature\Signature\SignedInfo\SignatureMethod
        SignatureMethodType signatureMethod = new SignatureMethodType();
        signatureMethod.setAlgorithm(messageDigestAlgorithm.getUri());
        signedInfo.setSignatureMethod(signatureMethod);
    }

    @Override
    public void add(String filename, String mimeType) {
        String id = String.format("ID_%s", signedInfo.getReference().size());

        // \XAdESSignature\Signature\SignedInfo\Reference
        ReferenceType reference = new ReferenceType();
        reference.setId(id);
        reference.setURI(filename);
        reference.setDigestValue(messageDigest.digest());

        // \XAdESSignature\Signature\SignedInfo\Reference\DigestMethod
        DigestMethodType digestMethodType = new DigestMethodType();
        digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
        reference.setDigestMethod(digestMethodType);

        signedInfo.getReference().add(reference);

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties\DataObjectFormat
        DataObjectFormatType dataObjectFormatType = new DataObjectFormatType();
        dataObjectFormatType.setObjectReference(String.format("#%s", id));
        dataObjectFormatType.setMimeType(mimeType);

        signedDataObjectProperties.getDataObjectFormat().add(dataObjectFormatType);
    }

    public byte[] toBytes(SignatureHelper signatureHelper) {
        // \XAdESSignature
        XAdESSignaturesType xAdESSignaturesType = new XAdESSignaturesType();

        // \XAdESSignature\Signature
        SignatureType signatureType = new SignatureType();
        signatureType.setId("Signature");
        signatureType.setSignedInfo(signedInfo);
        xAdESSignaturesType.getSignature().add(signatureType);

        // \XAdESSignature\Signature\KeyInfo
        KeyInfoType keyInfoType = new KeyInfoType();
        keyInfoType.getContent().add(getX509Data(signatureHelper));
        signatureType.setKeyInfo(keyInfoType);

        // \XAdESSignature\Signature\Object
        ObjectType objectType = new ObjectType();
        objectType.getContent().add(getQualifyingProperties(signatureHelper));
        signatureType.getObject().add(objectType);

        // TODO Generere signatur og legge signatur i dokumentet

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            JAXBElement<XAdESSignaturesType> jaxbRootElement = objectFactory.createXAdESSignatures(xAdESSignaturesType);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(jaxbRootElement, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the ASiCManifest into string output", e);
        }

    }

    private JAXBElement<X509DataType> getX509Data(SignatureHelper signatureHelper) {
        // \XAdESSignature\Signature\KeyInfo\X509Data
        X509DataType x509DataType = new X509DataType();
        // for (signatureHelper.getX509Certificate())

        // TODO Legge inn sertifikatbane

        return new org.w3._2000._09.xmldsig_.ObjectFactory().createX509Data(x509DataType);
    }

    private JAXBElement<QualifyingPropertiesType> getQualifyingProperties(SignatureHelper signatureHelper) {
        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties
        SignedSignaturePropertiesType signedSignaturePropertiesType = new SignedSignaturePropertiesType();
        try {
            signedSignaturePropertiesType.setSigningTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to use current DatatypeFactory", e);
        }

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate
        CertIDListType signingCertificate = new CertIDListType();
        signedSignaturePropertiesType.setSigningCertificate(signingCertificate);

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert
        CertIDType cert = new CertIDType();
        signingCertificate.getCert().add(cert);

        try {
            // Certificate
            X509CertImpl x509Cert = (X509CertImpl) signatureHelper.getX509Certificate();

            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\CertDigest
            DigestAlgAndValueType certDigest = new DigestAlgAndValueType();
            // certDigest.setDigestValue(x509Cert.getDig);
            certDigest.setDigestValue(DigestUtils.sha1Hex(x509Cert.getEncoded()).getBytes()); // TODO Ser ut til å være feil pasert på eksempel
            cert.setCertDigest(certDigest);

            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\CertDigest\DigestMethod
            DigestMethodType digestMethodType = new DigestMethodType();
            digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
            certDigest.setDigestMethod(digestMethodType);

            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\IssuerSerial
            X509IssuerSerialType issuerSerialType = new X509IssuerSerialType();
            issuerSerialType.setX509IssuerName(x509Cert.getIssuerX500Principal().getName());
            issuerSerialType.setX509SerialNumber(x509Cert.getSerialNumber());
            cert.setIssuerSerial(issuerSerialType);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Unable to encode certificate.", e);
        }

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties
        SignedPropertiesType signedPropertiesType = new SignedPropertiesType();
        signedPropertiesType.setId("SignedProperties");
        signedPropertiesType.setSignedSignatureProperties(signedSignaturePropertiesType);
        signedPropertiesType.setSignedDataObjectProperties(signedDataObjectProperties);

        // \XAdESSignature\Signature\Object\QualifyingProperties
        QualifyingPropertiesType qualifyingPropertiesType = new QualifyingPropertiesType();
        qualifyingPropertiesType.setSignedProperties(signedPropertiesType);

        // TODO Legge digest av SignedProperties i SignedInfo

        return new org.etsi.uri._01903.v1_3.ObjectFactory().createQualifyingProperties(qualifyingPropertiesType);
    }
}
