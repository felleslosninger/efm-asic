package no.difi.asic;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicContainerVerifierFactory {

    public static AsicContainerVerifierFactory newFactory() {
        return newFactory(MessageDigestAlgorithm.SHA256);
    }

    public static AsicContainerVerifierFactory newFactory(SignatureMethod signatureMethod) {
        return newFactory(signatureMethod.getMessageDigestAlgorithm());
    }

    protected static AsicContainerVerifierFactory newFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        return new AsicContainerVerifierFactory(messageDigestAlgorithm);
    }

    private MessageDigestAlgorithm messageDigestAlgorithm;

    private AsicContainerVerifierFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public AsicContainerVerifier verify(File file) throws IOException {
        return verify(file.toPath());
    }

    public AsicContainerVerifier verify(Path file) throws IOException {
        return verify(Files.newInputStream(file));
    }

    public AsicContainerVerifier verify(InputStream inputStream) throws IOException {
        return new AsicContainerVerifier(messageDigestAlgorithm, inputStream);
    }
}
