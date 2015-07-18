package no.difi.asic;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
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
import java.io.FileInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;


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
    private final File keyStoreFile;
    private final String keyStorePassword;
    private final String privateKeyPassword;
    private KeyStore keyStore;

    X509Certificate x509Certificate;

    public SignatureHelper(File keyStoreFile, String keyStorePassword, String privateKeyPassword) {
        this.keyStoreFile = keyStoreFile;
        this.keyStorePassword = keyStorePassword;
        this.privateKeyPassword = privateKeyPassword;
        Security.addProvider(new BouncyCastleProvider());
    }

    public byte[] signData(byte[] dataToSign) {

        List certList = new ArrayList();
        CMSProcessableByteArray msg = new CMSProcessableByteArray(dataToSign);

        KeyPair keyPair = getKeyPair(); // Reads private key and certificate from our own keystore


        certList.add(x509Certificate);
        String keyAlgorithm = keyPair.getPrivate().getAlgorithm();

        try {
            Store jcaCertStore = new JcaCertStore(certList);
            CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
            String signatureAlgorithm = "SHA1with" + keyAlgorithm;
            ContentSigner sha1Signer = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(keyPair.getPrivate());

            DigestCalculatorProvider digestCalculatorProvider = new JcaDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build();
            SignerInfoGenerator signerInfoGenerator = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider).build(sha1Signer, x509Certificate);
            cmsSignedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);
            cmsSignedDataGenerator.addCertificates(jcaCertStore);
            CMSSignedData sigData = cmsSignedDataGenerator.generate(msg, false);

            SignerInformationStore signerInfos = sigData.getSignerInfos();
            int size = signerInfos.size();


            byte[] bytes = Base64.encodeBase64(sigData.getEncoded());
            log.debug(new String(bytes));

            return sigData.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign " + e.getMessage(), e);
        }
    }


    KeyPair getKeyPair() {
        KeyStore keyStore = getKeyStore();
        String alias = null;
        try {
            alias = keyStore.aliases().nextElement();
            x509Certificate = (X509Certificate) keyStore.getCertificate(alias);

            Key key = keyStore.getKey(alias, privateKeyPassword.toCharArray());
            PrivateKey privateKey = (PrivateKey) key;

            return new KeyPair(x509Certificate.getPublicKey(), privateKey);
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Unable to retrieve next element from keystore " + e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            throw new IllegalStateException("Unable to get the private key from keystore " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to retrieve private key from keystore " + e.getMessage(), e);
        }
    }

    public synchronized KeyStore getKeyStore() {
        if (keyStore == null) {
            loadKeyStore();
        }
        return keyStore;
    }

    public void loadKeyStore() {
        try {
            if (keyStoreFile == null) {
                throw new IllegalStateException("KeyStore file not identified.");
            }
            FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile);

            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray()); // TODO: find password of keystore
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load keystore from file " + keyStoreFile + "; " + e.getMessage(), e);
        }
    }

}
