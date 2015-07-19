package no.difi.asic;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
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
import java.security.cert.X509Certificate;
import java.util.Arrays;


/**
 * Helper class to assist when creating a signature.
 *
 * Not thread safe
 *
 * @author steinar
 *         Date: 11.07.15
 *         Time: 22.53
 */
public class SignatureHelper {

    public static final Logger log = LoggerFactory.getLogger(SignatureHelper.class);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private X509Certificate x509Certificate;
    private KeyPair keyPair;

    // Helper method
    public SignatureHelper(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        this(keyStoreFile, keyStorePassword, null, keyPassword);
    }

    // Helper method
    public SignatureHelper(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        this(Files.newInputStream(keyStoreFile.toPath()), keyStorePassword, keyAlias, keyPassword);
    }

    /**
     * Loading keystore and fetching key
     * @param keyStoreStream Stream for keystore
     * @param keyStorePassword Password to open keystore
     * @param keyAlias Key alias, uses first key if set to null
     * @param keyPassword Key password
     */
    public SignatureHelper(InputStream keyStoreStream, String keyStorePassword, String keyAlias, String keyPassword) {
        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, keyStorePassword.toCharArray()); // TODO: find password of keystore

            keyStoreStream.close();

            if (keyAlias == null)
                keyAlias = keyStore.aliases().nextElement();
            x509Certificate = (X509Certificate) keyStore.getCertificate(keyAlias);

            Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) key;

            keyPair = new KeyPair(x509Certificate.getPublicKey(), privateKey);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Unable to retrieve next element from keystore " + e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            throw new IllegalStateException("Unable to get the private key from keystore " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to retrieve private key from keystore " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load keystore: " + e.getMessage(), e);
        }
    }

    /**
     * Sign content
     * @param dataToSign Content to be signed
     * @return Signature
     */
    public byte[] signData(byte[] dataToSign) {
        CMSProcessableByteArray msg = new CMSProcessableByteArray(dataToSign);

        String keyAlgorithm = keyPair.getPrivate().getAlgorithm();

        try {
            Store jcaCertStore = new JcaCertStore(Arrays.asList(x509Certificate));
            CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
            String signatureAlgorithm = "SHA1with" + keyAlgorithm;
            ContentSigner sha1Signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());

            DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build();
            SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(sha1Signer, x509Certificate);
            cmsSignedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);
            cmsSignedDataGenerator.addCertificates(jcaCertStore);
            CMSSignedData sigData = cmsSignedDataGenerator.generate(msg, false);

            /// SignerInformationStore signerInfos = sigData.getSignerInfos();
            // int size = signerInfos.size();

            byte[] bytes = Base64.encodeBase64(sigData.getEncoded());
            log.debug(new String(bytes));

            return sigData.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign " + e.getMessage(), e);
        }
    }

    public X509Certificate getX509Certificate() {
        return x509Certificate;
    }
}
