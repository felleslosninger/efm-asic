package no.difi.asic.extras;

import com.google.common.io.ByteStreams;
import no.difi.asic.AsicUtils;
import no.difi.asic.AsicWriter;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.TreeSet;

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
    public AsicWriter add(File file) throws IOException {
        return add(file.toPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(File file, String entryName) throws IOException {
        return add(file.toPath(), entryName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(Path path) throws IOException {
        return add(path, path.toFile().getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(Path path, String entryName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(InputStream inputStream, String filename) throws IOException {
        return add(inputStream, filename, AsicUtils.detectMime(filename));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(File file, String entryName, MimeType mimeType) throws IOException {
        return add(file.toPath(), entryName, mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsicWriter add(Path path, String entryName, MimeType mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            add(inputStream, entryName, mimeType);
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        try (InputStream inputStream = Files.newInputStream(path)) {
            addEncrypted(inputStream, entryName);
        }
        return this;
    }

    public AsicWriter addEncrypted(InputStream inputStream, String filename) throws IOException {
        return addEncrypted(inputStream, filename, AsicUtils.detectMime(filename));
    }

    public AsicWriter addEncrypted(File file, String entryName, MimeType mimeType) throws IOException {
        return addEncrypted(file.toPath(), entryName, mimeType);
    }

    public AsicWriter addEncrypted(Path path, String entryName, MimeType mimeType) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            addEncrypted(inputStream, entryName, mimeType);
        }
        return this;
    }

    public AsicWriter addEncrypted(InputStream inputStream, String filename, MimeType mimeType) throws IOException {
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

            return asicWriter.add(new ByteArrayInputStream(data.getEncoded()), filename + ".p7m", mimeType);
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public AsicWriter setRootEntryName(String name) {
        if (this.entryNeames.contains(name))
            name = String.format("%s.p7m", name);

        return asicWriter.setRootEntryName(name);
    }

    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyPassword) throws IOException {
        return asicWriter.sign(keyStoreFile, keyStorePassword, keyPassword);
    }

    @Override
    public AsicWriter sign(File keyStoreFile, String keyStorePassword, String keyAlias, String keyPassword) throws IOException {
        return asicWriter.sign(keyStoreFile, keyStorePassword, keyAlias, keyPassword);
    }

    @Override
    public AsicWriter sign(SignatureHelper signatureHelper) throws IOException {
        return asicWriter.sign(signatureHelper);
    }
}
