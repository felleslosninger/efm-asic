package no.difi.asic.extras;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;

import com.google.common.io.ByteStreams;

import no.difi.asic.AsicUtils;
import no.difi.asic.AsicWriter;
import no.difi.asic.KeyStoreType;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;

/**
 * Wrapper to seamlessly encode specific files.
 */
public class CmsEncryptedAsicWriter extends CmsEncryptedAsicAbstract implements AsicWriter {

    private AsicWriter asicWriter;
    private X509Certificate certificate;
    private ASN1ObjectIdentifier cmsAlgorithm;

    private Set<String> entryNeames = new TreeSet<>();

    public CmsEncryptedAsicWriter(AsicWriter asicWriter, X509Certificate certificate) {
        this(asicWriter, certificate,  CMSAlgorithm.AES256_GCM);
    }

    public CmsEncryptedAsicWriter(AsicWriter asicWriter, X509Certificate certificate, ASN1ObjectIdentifier cmsAlgorithm) {
        this.asicWriter = asicWriter;
        this.certificate = certificate;
        this.cmsAlgorithm = cmsAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(File file, String entryName) throws IOException {
        return add(file.toPath(), entryName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(Path path, String entryName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(InputStream inputStream, String filename) throws IOException {
        return add(inputStream, filename, AsicUtils.detectMime(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(File file, String entryName, MimeType mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName, mimeType);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CmsEncryptedAsicWriter add(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        asicWriter.add(inputStream, filename, mimeType);
        return this;
    }

    public CmsEncryptedAsicWriter addEncrypted(File file) throws IOException {
        return addEncrypted(file.toPath());
    }

    public CmsEncryptedAsicWriter addEncrypted(File file, String entryName) throws IOException {
        return addEncrypted(file.toPath(), entryName);
    }

    public CmsEncryptedAsicWriter addEncrypted(Path path) throws IOException {
        return addEncrypted(path, path.toFile().getName());
    }

    public CmsEncryptedAsicWriter addEncrypted(Path path, String entryName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            addEncrypted(inputStream, entryName);
        }
        return this;
    }

    public CmsEncryptedAsicWriter addEncrypted(InputStream inputStream, String filename) throws IOException {
        return addEncrypted(inputStream, filename, AsicUtils.detectMime(filename));
    }

    public CmsEncryptedAsicWriter addEncrypted(File file, String entryName, MimeType mimeType) throws IOException {
        return addEncrypted(file.toPath(), entryName, mimeType);
    }

    public CmsEncryptedAsicWriter addEncrypted(Path path, String entryName, MimeType mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            addEncrypted(inputStream, entryName, mimeType);
        }
        return this;
    }

    public CmsEncryptedAsicWriter addEncrypted(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ByteStreams.copy(inputStream, byteArrayOutputStream);

            CMSEnvelopedDataGenerator cmsEnvelopedDataGenerator = new CMSEnvelopedDataGenerator();
            cmsEnvelopedDataGenerator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(certificate).setProvider(BC));
            CMSEnvelopedData data = cmsEnvelopedDataGenerator.generate(
                    new CMSProcessableByteArray(byteArrayOutputStream.toByteArray()),
                    new JceCMSContentEncryptorBuilder(cmsAlgorithm).setProvider(BC).build()
            );

            this.entryNeames.add(filename);

            asicWriter.add(new ByteArrayInputStream(data.getEncoded()), filename + ".p7m", mimeType);
            return this;
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public CmsEncryptedAsicWriter setRootEntryName(String name) {
        if (this.entryNeames.contains(name))
            name = String.format("%s.p7m", name);

        asicWriter.setRootEntryName(name);
        return this;
    }

    @Override
    public CmsEncryptedAsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        asicWriter.sign(keyStoreFile, keyStorePassword, keyPassword);
        return this;
    }

    @Override
    public CmsEncryptedAsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        asicWriter.sign(keyStoreFile, keyStorePassword, keyAlias, keyPassword);
        return this;
    }

    @Override
    public CmsEncryptedAsicWriter sign(SignatureHelper signatureHelper) throws IOException {
        asicWriter.sign(signatureHelper);
        return this;
    }

    @Override
    public CmsEncryptedAsicWriter sign (File keyStoreFile, String keyStorePassword, KeyStoreType keyStoreType, String keyAlias, String keyPassword) throws IOException {
        asicWriter.sign(keyStoreFile, keyStorePassword, keyStoreType, keyAlias, keyPassword);
        return this;
    }
}
