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

    public void verify(File file) throws IOException {
        verify(file.toPath());
    }

    public void verify(Path file) throws IOException {
        InputStream inputStream = Files.newInputStream(file);
        verify(inputStream);
        inputStream.close();
    }

    public void verify(InputStream inputStream) throws IOException {
        new AsicContainerVerifier(messageDigestAlgorithm, inputStream);
    }
}
