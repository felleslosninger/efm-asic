package no.difi.asic.extras;

import com.google.common.io.ByteStreams;
import no.difi.asic.AsicReader;
import no.difi.commons.asic.jaxb.asic.AsicManifest;
import no.difi.commons.asic.jaxb.asic.Certificate;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper to seamlessly decode encoded files.
 */
public class CmsEncryptedAsicReader extends CmsEncryptedAsicAbstract implements AsicReader {

    private AsicReader asicReader;
    private PrivateKey privateKey;

    private String currentFile;

    public CmsEncryptedAsicReader(AsicReader asicReader, PrivateKey privateKey) {
        this.asicReader = asicReader;
        this.privateKey = privateKey;
    }

    @Override
    public String getNextFile() throws IOException {
        currentFile = asicReader.getNextFile();
        if (currentFile == null)
            return null;

        return currentFile.endsWith(".p7m") ? currentFile.substring(0, currentFile.length() - 4) : currentFile;
    }

    @Override
    public void writeFile(File file) throws IOException {
        writeFile(file.toPath());
    }

    @Override
    public void writeFile(Path path) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            writeFile(outputStream);
        }
    }

    @Override
    public void writeFile(OutputStream outputStream) throws IOException {
        if (currentFile.endsWith(".p7m")) {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                asicReader.writeFile(byteArrayOutputStream);

                CMSEnvelopedDataParser cmsEnvelopedDataParser = new CMSEnvelopedDataParser(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                // expect exactly one recipient
                Collection<?> recipients = cmsEnvelopedDataParser.getRecipientInfos().getRecipients();
                if (recipients.size() != 1)
                    throw new IllegalArgumentException();

                // retrieve recipient and decode it
                RecipientInformation recipient = (RecipientInformation) recipients.iterator().next();
                byte[] decryptedData = recipient.getContent(new JceKeyTransEnvelopedRecipient(privateKey).setProvider(BC));

                ByteStreams.copy(new ByteArrayInputStream(decryptedData), outputStream);
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        } else {
            asicReader.writeFile(outputStream);
        }
    }

    @Override
    public InputStream inputStream() throws IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();
        PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);

        writeFile(pipedOutputStream);
        return pipedInputStream;
    }

    @Override
    public void close() throws IOException {
        asicReader.close();
    }

    @Override
    public AsicManifest getAsicManifest() {
        AsicManifest asicManifest = asicReader.getAsicManifest();

        String rootfile = asicManifest.getRootfile();
        if (rootfile != null && rootfile.endsWith(".p7m"))
            asicManifest.setRootfile(rootfile.substring(0, rootfile.length() - 4));

        return asicManifest;
    }

    @Override
    public List<Certificate> getCertificateChain() {
        return asicReader.getCertificateChain();
    }
}
