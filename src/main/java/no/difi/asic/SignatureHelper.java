package no.difi.asic;

import com.google.common.io.BaseEncoding;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;


/**
 * Helper class to assist when creating a signature.
 * <p>
 * Not thread safe
 *
 * @author steinar
 *         Date: 11.07.15
 *         Time: 22.53
 */
public class SignatureHelper {

    private static final Logger logger = LoggerFactory.getLogger(SignatureHelper.class);

    private static JcaSimpleSignerInfoVerifierBuilder jcaSimpleSignerInfoVerifierBuilder =
            new JcaSimpleSignerInfoVerifierBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    private static JcaDigestCalculatorProviderBuilder jcaDigestCalculatorProviderBuilder =
            new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private X509Certificate x509Certificate;
    private java.security.cert.Certificate[] certificateChain;
    private KeyPair keyPair;

    private JcaContentSignerBuilder jcaContentSignerBuilder;

    /**
     * Loads the keystore and obtains the private key, the public key and the associated certificate
     */
    public SignatureHelper(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        this(keyStoreFile, keyStorePassword, null, keyPassword);
    }

    /**
     * Loads the keystore and obtains the private key, the public key and the associated certificate referenced by the alias.
     *
     * @param keyStoreFile     file holding the JKS keystore.
     * @param keyStorePassword password of the key store itself
     * @param keyAlias         the alias referencing the private and public key pair.
     * @param keyPassword      password protecting the private key
     * @throws IOException
     */
    public SignatureHelper(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        this(Files.newInputStream(keyStoreFile.toPath()), keyStorePassword, keyAlias, keyPassword);
    }

    /**
     * Loading keystore and fetching key
     *
     * @param keyStoreStream   Stream for keystore
     * @param keyStorePassword Password to open keystore
     * @param keyAlias         Key alias, uses first key if set to null
     * @param keyPassword      Key password
     */
    public SignatureHelper(InputStream keyStoreStream, String keyStorePassword, String keyAlias, String keyPassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, keyStorePassword.toCharArray()); // TODO: find password of keystore

            keyStoreStream.close();

            if (keyAlias == null)
                keyAlias = keyStore.aliases().nextElement();
            x509Certificate = (X509Certificate) keyStore.getCertificate(keyAlias);

            certificateChain = keyStore.getCertificateChain(keyAlias);

            Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) key;

            keyPair = new KeyPair(x509Certificate.getPublicKey(), privateKey);

            jcaContentSignerBuilder = new JcaContentSignerBuilder(String.format("SHA1with%s", privateKey.getAlgorithm()))
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to retrieve private key from keystore: %s", e.getMessage()), e);
        }
    }

    /**
     * Sign content
     *
     * @param data Content to be signed
     * @return Signature
     */
    byte[] signData(byte[] data) {
        try {
            DigestCalculatorProvider digestCalculatorProvider = jcaDigestCalculatorProviderBuilder.build();
            ContentSigner contentSigner = jcaContentSignerBuilder.build(keyPair.getPrivate());
            SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(contentSigner, x509Certificate);

            CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
            cmsSignedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);
            cmsSignedDataGenerator.addCertificates(new JcaCertStore(Arrays.asList(x509Certificate)));
            CMSSignedData cmsSignedData = cmsSignedDataGenerator.generate(new CMSProcessableByteArray(data), false);

            logger.debug(BaseEncoding.base64().encode(cmsSignedData.getEncoded()));
            return cmsSignedData.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    static no.difi.xsd.asic.model._1.Certificate validate(byte[] data, byte[] signature) {
        no.difi.xsd.asic.model._1.Certificate certificate = null;

        try {
            CMSSignedData cmsSignedData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
            Store store = cmsSignedData.getCertificates();
            SignerInformationStore signerInformationStore = cmsSignedData.getSignerInfos();

            for (SignerInformation signerInformation : signerInformationStore.getSigners()) {
                X509CertificateHolder x509Certificate = (X509CertificateHolder) store.getMatches(signerInformation.getSID()).iterator().next();
                logger.info(x509Certificate.getSubject().toString());

                if (signerInformation.verify(jcaSimpleSignerInfoVerifierBuilder.build(x509Certificate))) {
                    certificate = new no.difi.xsd.asic.model._1.Certificate();
                    certificate.setCertificate(x509Certificate.getEncoded());
                    certificate.setSubject(x509Certificate.getSubject().toString());
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            certificate = null;
        }

        if (certificate == null)
            throw new IllegalStateException("Unable to verify signature.");

        return certificate;
    }

    X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    Certificate[] getCertificateChain() {
        return certificateChain;
    }
}
