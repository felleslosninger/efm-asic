package no.difi.asic;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class AsicVerifierFactory {

    public static AsicVerifierFactory newFactory() {
        return newFactory(MessageDigestAlgorithm.SHA256);
    }

    public static AsicVerifierFactory newFactory(SignatureMethod signatureMethod) {
        return newFactory(signatureMethod.getMessageDigestAlgorithm());
    }

    protected static AsicVerifierFactory newFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        return new AsicVerifierFactory(messageDigestAlgorithm);
    }

    private MessageDigestAlgorithm messageDigestAlgorithm;

    private AsicVerifierFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public AsicVerifier verify(File file) throws IOException {
        return verify(file.toPath());
    }

    public AsicVerifier verify(Path file) throws IOException {
        return verify(Files.newInputStream(file));
    }

    public AsicVerifier verify(InputStream inputStream) throws IOException {
        return new AsicVerifier(messageDigestAlgorithm, inputStream);
    }
}
