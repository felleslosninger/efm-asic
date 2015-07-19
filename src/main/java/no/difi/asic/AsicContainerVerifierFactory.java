package no.difi.asic;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class AsicContainerVerifierFactory {

    public static AsicContainerVerifierFactory newFactory() {
        return newFactory(MessageDigestAlgorithm.SHA256);
    }

    public static AsicContainerVerifierFactory newFactory(SignatureMethod signatureMethod) {
        return newFactory(signatureMethod.getMessageDigestAlgorithm());
    }

    public static AsicContainerVerifierFactory newFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        return new AsicContainerVerifierFactory(messageDigestAlgorithm);
    }

    private MessageDigestAlgorithm messageDigestAlgorithm;

    private AsicContainerVerifierFactory(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public void verify(InputStream inputStream) throws IOException {
        verify(inputStream, null);
    }

    public void verify(InputStream inputStream, Path outputFolder) throws IOException {
        new AsicContainerVerifier(messageDigestAlgorithm, inputStream, outputFolder);
    }
}
