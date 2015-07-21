package no.difi.asic;

public enum SignatureMethod {
    CAdES(MessageDigestAlgorithm.SHA256),
    XAdES(MessageDigestAlgorithm.SHA256),
    ;

    private MessageDigestAlgorithm messageDigestAlgorithm;

    SignatureMethod(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public MessageDigestAlgorithm getMessageDigestAlgorithm() {
        return messageDigestAlgorithm;
    }
}
