package no.difi.asic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

abstract class AbstractAsicManifest {

    protected MessageDigestAlgorithm messageDigestAlgorithm;
    protected MessageDigest messageDigest;

    public AbstractAsicManifest(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;

        // Create message digest
        try {
            messageDigest = MessageDigest.getInstance(messageDigestAlgorithm.getAlgorithm());
            messageDigest.reset();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(String.format("Algorithm %s not supported", messageDigestAlgorithm.getAlgorithm()), e);
        }
    }

    /**
     * @inheritDoc
     */
    public MessageDigest getMessageDigest() {
        messageDigest.reset();
        return messageDigest;
    }

    /**
     * @inheritDoc
     */
    public abstract void add(String filename, MimeType mimeType);
}
