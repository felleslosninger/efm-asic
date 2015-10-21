package no.difi.asic.extras;

import no.difi.asic.AsicUtils;
import no.difi.asic.AsicWriter;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.security.cert.X509Certificate;

/**
 * Wrapper to seamlessly encode specific files.
 */
public class CmsEncryptedAsicWriter implements AsicWriter {

    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }

    private AsicWriter asicWriter;
    private X509Certificate certificate;
    private ASN1ObjectIdentifier cmsAlgorithm;

    public CmsEncryptedAsicWriter(AsicWriter asicWriter, X509Certificate certificate) {
        this(asicWriter, certificate,  CMSAlgorithm.AES128_CBC);
    }

    public CmsEncryptedAsicWriter(AsicWriter asicWriter, X509Certificate certificate, ASN1ObjectIdentifier cmsAlgorithm) {
        this.asicWriter = asicWriter;
        this.certificate = certificate;
        this.cmsAlgorithm = cmsAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(File file, String entryName) throws IOException {
        return add(file.toPath(), entryName);
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(Path path, String entryName) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, entryName);
        inputStream.close();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(InputStream inputStream, String filename) throws IOException {
        return add(inputStream, filename, AsicUtils.detectMime(filename));
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(File file, String entryName, MimeType mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    /**
     * {@inheritDoc}
     */
    public AsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        add(inputStream, entryName, mimeType);
        inputStream.close();
        return this;
    }

    public AsicWriter add(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        return asicWriter.add(inputStream, filename, mimeType);
    }

    public AsicWriter addEncrypted(File file) throws IOException {
        return addEncrypted(file.toPath());
    }

    public AsicWriter addEncrypted(File file, String entryName) throws IOException {
        return addEncrypted(file.toPath(), entryName);
    }

    public AsicWriter addEncrypted(Path path) throws IOException {
        return addEncrypted(path, path.toFile().getName());
    }

    public AsicWriter addEncrypted(Path path, String entryName) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        addEncrypted(inputStream, entryName);
        inputStream.close();
        return this;
    }

    public AsicWriter addEncrypted(InputStream inputStream, String filename) throws IOException {
        return addEncrypted(inputStream, filename, AsicUtils.detectMime(filename));
    }

    public AsicWriter addEncrypted(File file, String entryName, MimeType mimeType) throws IOException {
        return addEncrypted(file.toPath(), entryName, mimeType);
    }

    public AsicWriter addEncrypted(Path path, String entryName, MimeType mimeType) throws IOException {
        InputStream inputStream = Files.newInputStream(path);
        addEncrypted(inputStream, entryName, mimeType);
        inputStream.close();
        return this;
    }

    public AsicWriter addEncrypted(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, byteArrayOutputStream);

            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(certificate).setProvider(BC));
            CMSEnvelopedData data = cmsEnvelopedDataGenerator.generate(
                    new CMSProcessableByteArray(byteArrayOutputStream.toByteArray()),
                    new JceCMSContentEncryptorBuilder(cmsAlgorithm).setProvider(BC).build()
            );

            return asicWriter.add(new ByteArrayInputStream(data.getEncoded()), filename + ".p7m", mimeType);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public AsicWriter setRootEntryName(String name) {
        return asicWriter.setRootEntryName(name);
    }

    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        return asicWriter.sign(keyStoreFile, keyStorePassword, keyPassword);
    }

    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        return asicWriter.sign(keyStoreFile, keyStorePassword, keyAlias, keyPassword);
    }

    public AsicWriter sign(SignatureHelper signatureHelper) throws IOException {
        return asicWriter.sign(signatureHelper);
    }
}
