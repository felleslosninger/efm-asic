package no.difi.asic;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicContainerReaderFactory {

    public static AsicContainerReaderFactory newFactory() {
        return newFactory(MessageDigestAlgorithm.SHA256);
    }

    public static AsicContainerReaderFactory newFactory(SignatureMethod signatureMethod) {
        return newFactory(signatureMethod.getMessageDigestAlgorithm());
    }

    protected static AsicContainerReaderFactory newFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        return new AsicContainerReaderFactory(messageDigestAlgorithm);
    }

    private MessageDigestAlgorithm messageDigestAlgorithm;

    private AsicContainerReaderFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public AsicContainerReader open(File file) throws IOException {
        return open(file.toPath());
    }

    public AsicContainerReader open(Path file) throws IOException {
        return open(Files.newInputStream(file));
    }

    public AsicContainerReader open(InputStream inputStream) throws IOException {
        return new AsicContainerReader(messageDigestAlgorithm, inputStream);
    }
}
