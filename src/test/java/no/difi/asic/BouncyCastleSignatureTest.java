package no.difi.asic;

import com.google.common.io.BaseEncoding;
import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author steinar
 *         Date: 05.07.15
 *         Time: 21.57
 */
public class BouncyCastleSignatureTest {


    private KeyPair keyPair;
    private X509Certificate x509Certificate;

    public static final Logger log = LoggerFactory.getLogger(BouncyCastleSignatureTest.class);

    @BeforeTest

    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void createSignature() throws Exception {

        List certList = new ArrayList();
        CMSProcessableByteArray msg = new CMSProcessableByteArray("Hello world".getBytes());
        // generateKeyPairAndCertificate();
        keyPair = getKeyPair(); // Reads private key and certificate from our own keystore


        String keyAlgorithm = keyPair.getPrivate().getAlgorithm();

        Store jcaCertStore = new JcaCertStore(certList);
        CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
        String signatureAlgorithm = "SHA1with" + keyAlgorithm;
        ContentSigner sha1Signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());

        DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build();
        SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(sha1Signer, x509Certificate);
        cmsSignedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);
        cmsSignedDataGenerator.addCertificates(jcaCertStore);
        CMSSignedData sigData = cmsSignedDataGenerator.generate(msg, false);

        byte[] bytes = BaseEncoding.base64().encode(sigData.getEncoded()).getBytes();
        log.debug(new String(bytes));

    }

    void generateKeyPairAndCertificate() throws NoSuchProviderException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, SignatureException, InvalidKeyException {

        String BC = BouncyCastleProvider.PROVIDER_NAME;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA", BC);
        keyPairGenerator.initialize(1024, new SecureRandom());
        keyPair = keyPairGenerator.generateKeyPair();

        X500NameBuilder nameBuilder = createStdBuilder();


        ContentSigner sigGen = new JcaContentSignerBuilder("SHA1withDSA").setProvider(BC).build(keyPair.getPrivate());
        JcaX509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                nameBuilder.build(),
                BigInteger.valueOf(1),
                new Date(System.currentTimeMillis() - 50000),
                new Date(System.currentTimeMillis() + 50000),
                nameBuilder.build(),
                keyPair.getPublic());


        x509Certificate = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certGen.build(sigGen));

        x509Certificate.checkValidity(new Date());

        x509Certificate.verify(keyPair.getPublic());

        ByteArrayInputStream bIn = new ByteArrayInputStream(x509Certificate.getEncoded());
        CertificateFactory fact = CertificateFactory.getInstance("X.509", BC);

        x509Certificate = (X509Certificate)fact.generateCertificate(bIn);

        System.out.println(x509Certificate);
    }

    private X500NameBuilder createStdBuilder()
    {
        X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

        builder.addRDN(BCStyle.C, "AU");
        builder.addRDN(BCStyle.O, "The Legion of the Bouncy Castle");
        builder.addRDN(BCStyle.L, "Melbourne");
        builder.addRDN(BCStyle.ST, "Victoria");
        builder.addRDN(BCStyle.E, "feedback-crypto@bouncycastle.org");

        return builder;
    }

    KeyPair getKeyPair() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance("JKS");

        FileInputStream fileInputStream = new FileInputStream(new File("src/test/resources/kontaktinfo-client-test.jks"));


        keyStore.load(fileInputStream, "changeit".toCharArray());

        String alias = keyStore.aliases().nextElement();
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

        x509Certificate = certificate;

        Key key = keyStore.getKey(alias, "changeit".toCharArray());
        PrivateKey privateKey = (PrivateKey) key;

        return new KeyPair(certificate.getPublicKey(), privateKey);
    }

}
