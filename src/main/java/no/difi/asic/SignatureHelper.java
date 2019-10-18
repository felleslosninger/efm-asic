package no.difi.asic;

import com.google.common.io.BaseEncoding;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;


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

    protected final Provider provider;

    protected final JcaDigestCalculatorProviderBuilder jcaDigestCalculatorProviderBuilder;

    protected X509Certificate x509Certificate;

    protected java.security.cert.Certificate[] certificateChain;

    protected KeyPair keyPair;

    protected JcaContentSignerBuilder jcaContentSignerBuilder;

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
     */
    public SignatureHelper(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        this(BCHelper.getProvider());
        try (InputStream inputStream = Files.newInputStream(keyStoreFile.toPath())) {
            loadCertificate(loadKeyStore(inputStream, keyStorePassword), keyAlias, keyPassword);
        }
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
        this(BCHelper.getProvider());
        loadCertificate(loadKeyStore(keyStoreStream, keyStorePassword), keyAlias, keyPassword);
    }

    /**
     * Loading keystore
     *
     * @param certificate      Public part of signing key
     * @param privateKey       Private signing key
     */
    public SignatureHelper(X509Certificate certificate, PrivateKey privateKey, String signatureAlgorithm) {
        this(BCHelper.getProvider());
        loadCertificate(certificate, privateKey, signatureAlgorithm);
    }

    protected SignatureHelper(Provider provider) {
        this.provider = provider;

        jcaDigestCalculatorProviderBuilder = new JcaDigestCalculatorProviderBuilder();
        if (provider != null)
            jcaDigestCalculatorProviderBuilder.setProvider(provider);
    }

    protected KeyStore loadKeyStore(InputStream keyStoreStream, String keyStorePassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, keyStorePassword.toCharArray());

            return keyStore;
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Load keystore; %s", e.getMessage()), e);
        }
    }

    protected void loadCertificate(KeyStore keyStore, String keyAlias, String keyPassword) {
        try {
            if (keyAlias == null)
                keyAlias = keyStore.aliases().nextElement();
            certificateChain = keyStore.getCertificateChain(keyAlias);

            Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) key;

            loadCertificate((X509Certificate) keyStore.getCertificate(keyAlias), privateKey, String.format("SHA1with%s", privateKey.getAlgorithm()));
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to retrieve private key from keystore: %s", e.getMessage()), e);
        }
    }

    protected void loadCertificate(X509Certificate x509Certificate, PrivateKey privateKey, String signatureAlgorithm) {
        this.keyPair = new KeyPair(x509Certificate.getPublicKey(), privateKey);
        this.x509Certificate = x509Certificate;
        this.jcaContentSignerBuilder = new JcaContentSignerBuilder(signatureAlgorithm);

        if (provider != null)
            jcaContentSignerBuilder.setProvider(provider);
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
            cmsSignedDataGenerator.addCertificates(new JcaCertStore(Collections.singletonList(x509Certificate)));
            CMSSignedData cmsSignedData = cmsSignedDataGenerator.generate(new CMSProcessableByteArray(data), false);

            if(logger.isDebugEnabled()) {
                logger.debug(BaseEncoding.base64().encode(cmsSignedData.getEncoded()));
            }
            return cmsSignedData.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Unable to sign: %s", e.getMessage()), e);
        }
    }


    X509Certificate getX509Certificate() {
        return x509Certificate;
    }

    Certificate[] getCertificateChain() {
        return certificateChain;
    }
}
