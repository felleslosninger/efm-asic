package no.difi.asic;

import org.apache.commons.codec.digest.DigestUtils;
import org.etsi.uri._01903.v1_3.*;
import org.etsi.uri._02918.v1_2.XAdESSignatures;
import org.w3._2000._09.xmldsig_.*;
import org.w3._2000._09.xmldsig_.Object;
import org.w3._2000._09.xmldsig_.SignatureMethod;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.GregorianCalendar;

class XadesAsicManifest extends AbstractAsicManifest {

    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBContext.newInstance(XAdESSignatures.class, X509Data.class, QualifyingProperties.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    // \XAdESSignature\Signature\SignedInfo
    private SignedInfo signedInfo;
    // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties
    private SignedDataObjectProperties signedDataObjectProperties = new SignedDataObjectProperties();

    public XadesAsicManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        super(messageDigestAlgorithm);

        // \XAdESSignature\Signature\SignedInfo
        signedInfo = new SignedInfo();

        // \XAdESSignature\Signature\SignedInfo\CanonicalizationMethod
        CanonicalizationMethod canonicalizationMethod = new CanonicalizationMethod();
        canonicalizationMethod.setAlgorithm("http://www.w3.org/2006/12/xml-c14n11");
        signedInfo.setCanonicalizationMethod(canonicalizationMethod);

        // \XAdESSignature\Signature\SignedInfo\SignatureMethod
        SignatureMethod signatureMethod = new SignatureMethod();
        signatureMethod.setAlgorithm(messageDigestAlgorithm.getUri());
        signedInfo.setSignatureMethod(signatureMethod);
    }

    @Override
    public void add(String filename, MimeType mimeType) {
        String id = String.format("ID_%s", signedInfo.getReferences().size());

        {
            // \XAdESSignature\Signature\SignedInfo\Reference
            Reference reference = new Reference();
            reference.setId(id);
            reference.setURI(filename);
            reference.setDigestValue(messageDigest.digest());

            // \XAdESSignature\Signature\SignedInfo\Reference\DigestMethod
            DigestMethod digestMethodType = new DigestMethod();
            digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
            reference.setDigestMethod(digestMethodType);

            signedInfo.getReferences().add(reference);
        }

        {
            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedDataObjectProperties\DataObjectFormat
            DataObjectFormat dataObjectFormatType = new DataObjectFormat();
            dataObjectFormatType.setObjectReference(String.format("#%s", id));
            dataObjectFormatType.setMimeType(mimeType.toString());

            signedDataObjectProperties.getDataObjectFormats().add(dataObjectFormatType);
        }
    }

    public byte[] toBytes(SignatureHelper signatureHelper) {
        // \XAdESSignature
        XAdESSignatures xAdESSignaturesType = new XAdESSignatures();

        // \XAdESSignature\Signature
        Signature signatureType = new Signature();
        signatureType.setId("Signature");
        signatureType.setSignedInfo(signedInfo);
        xAdESSignaturesType.getSignatures().add(signatureType);

        // \XAdESSignature\Signature\KeyInfo
        KeyInfo keyInfoType = new KeyInfo();
        keyInfoType.getContent().add(getX509Data(signatureHelper));
        signatureType.setKeyInfo(keyInfoType);

        // \XAdESSignature\Signature\Object
        Object objectType = new Object();
        objectType.getContent().add(getQualifyingProperties(signatureHelper));
        signatureType.getObjects().add(objectType);

        // \XAdESSignature\Signature\Object\SignatureValue
        signatureType.setSignatureValue(getSignature());

        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            marshaller.marshal(xAdESSignaturesType, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to marshall the XAdESSignature into string output", e);
        }

    }

    private X509Data getX509Data(SignatureHelper signatureHelper) {
        org.w3._2000._09.xmldsig_.ObjectFactory objectFactory = new org.w3._2000._09.xmldsig_.ObjectFactory();

        // \XAdESSignature\Signature\KeyInfo\X509Data
        X509Data x509DataType = new X509Data();

        for (Certificate certificate : signatureHelper.getCertificateChain()) {
            try {
                // \XAdESSignature\Signature\KeyInfo\X509Data\X509Certificate
                x509DataType.getX509IssuerSerialsAndX509SKISAndX509SubjectNames().add(objectFactory.createX509DataX509Certificate(certificate.getEncoded()));
            } catch (CertificateEncodingException e) {
                throw new IllegalStateException("Unable to insert certificate.");
            }
        }

        return x509DataType;
    }

    private QualifyingProperties getQualifyingProperties(SignatureHelper signatureHelper) {
        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties
        SignedSignatureProperties signedSignaturePropertiesType = new SignedSignatureProperties();
        try {
            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningTime
            signedSignaturePropertiesType.setSigningTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Unable to use current DatatypeFactory", e);
        }

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate
        SigningCertificate signingCertificate = new SigningCertificate();
        signedSignaturePropertiesType.setSigningCertificate(signingCertificate);

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert
        CertIDType cert = new CertIDType();
        signingCertificate.getCerts().add(cert);

        try {
            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\CertDigest
            DigestAlgAndValueType certDigest = new DigestAlgAndValueType();
            certDigest.setDigestValue(DigestUtils.sha1(signatureHelper.getX509Certificate().getEncoded()));
            cert.setCertDigest(certDigest);

            // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\CertDigest\DigestMethod
            DigestMethod digestMethodType = new DigestMethod();
            digestMethodType.setAlgorithm("http://www.w3.org/2000/09/xmldsig#sha1");
            certDigest.setDigestMethod(digestMethodType);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Unable to encode certificate.", e);
        }

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties\SignedSignatureProperties\SigningCertificate\Cert\IssuerSerial
        X509IssuerSerialType issuerSerialType = new X509IssuerSerialType();
        issuerSerialType.setX509IssuerName(signatureHelper.getX509Certificate().getIssuerX500Principal().getName());
        issuerSerialType.setX509SerialNumber(signatureHelper.getX509Certificate().getSerialNumber());
        cert.setIssuerSerial(issuerSerialType);

        // \XAdESSignature\Signature\Object\QualifyingProperties\SignedProperties
        SignedProperties signedPropertiesType = new SignedProperties();
        signedPropertiesType.setId("SignedProperties");
        signedPropertiesType.setSignedSignatureProperties(signedSignaturePropertiesType);
        signedPropertiesType.setSignedDataObjectProperties(signedDataObjectProperties);

        // \XAdESSignature\Signature\Object\QualifyingProperties
        QualifyingProperties qualifyingPropertiesType = new QualifyingProperties();
        qualifyingPropertiesType.setSignedProperties(signedPropertiesType);
        qualifyingPropertiesType.setTarget("#Signature");

        // Adding digest of SignedProperties into SignedInfo
        {
            // \XAdESSignature\Signature\SignedInfo\Reference
            Reference reference = new Reference();
            reference.setType("http://uri.etsi.org/01903#SignedProperties");
            reference.setURI("#SignedProperties");
            // TODO Generate digest

            // \XAdESSignature\Signature\SignedInfo\Reference\Transforms
            Transforms transformsType = new Transforms();
            reference.setTransforms(transformsType);

            // \XAdESSignature\Signature\SignedInfo\Reference\Transforms\Transform
            Transform transformType = new Transform();
            transformType.setAlgorithm("http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
            reference.getTransforms().getTransforms().add(transformType);

            // \XAdESSignature\Signature\SignedInfo\Reference\DigestMethod
            DigestMethod digestMethodType = new DigestMethod();
            digestMethodType.setAlgorithm(messageDigestAlgorithm.getUri());
            reference.setDigestMethod(digestMethodType);

            signedInfo.getReferences().add(reference);
        }

        return qualifyingPropertiesType;
    }

    protected SignatureValue getSignature() {
        // TODO Generate signature
        // http://stackoverflow.com/questions/30596933/xades-bes-detached-signedproperties-reference-wrong-digestvalue-java

        /*
        DigestMethod dm = fac.newDigestMethod(DigestMethod.SHA1, null);
        CanonicalizationMethod cn = fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,(C14NMethodParameterSpec) null);

        List<Reference> refs = new ArrayList<Reference>();
        Reference ref1 = fac.newReference(pathName, dm,null,null,signedRefID,messageDigest2.digest(datax));
        refs.add(ref1);

        Canonicalizer cn14 = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N11_OMIT_COMMENTS);
        byte[] canon;
        canon = cn14.canonicalizeSubtree(SPElement);
        Reference ref2 = fac.newReference("#"+signedPropID,dm, null , sigProp , signedPropRefID,messageDigest2.digest(canon));
        refs.add(ref2);

        SignatureMethod sm = fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
        SignedInfo si = fac.newSignedInfo(cn, sm, refs);

        XMLSignature signature = fac.newXMLSignature(si, ki,objects,signatureID,null);

        signature.sign(dsc);
        */

        SignatureValue signatureValue = new SignatureValue();
        return signatureValue;
    }

    @SuppressWarnings("unchecked")
    public static void extractAndVerify(String xml, ManifestVerifier manifestVerifier) {
        // Updating namespace
        xml = xml.replace("http://uri.etsi.org/02918/v1.1.1#", "http://uri.etsi.org/02918/v1.2.1#");
        xml = xml.replace("http://uri.etsi.org/2918/v1.2.1#", "http://uri.etsi.org/02918/v1.2.1#");

        XAdESSignatures manifest;

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            manifest = (XAdESSignatures) unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read content as XML", e);
        }

        for (Signature signature : manifest.getSignatures()) {
            SignedInfo signedInfoType = signature.getSignedInfo();

            for (Reference reference : signedInfoType.getReferences()) {
                if (!reference.getURI().startsWith("#"))
                    manifestVerifier.update(reference.getURI(), null, reference.getDigestValue(), reference.getDigestMethod().getAlgorithm(), null);
            }
        }
    }
}
