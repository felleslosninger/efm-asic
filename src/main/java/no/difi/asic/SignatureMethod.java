package no.difi.asic;

public enum SignatureMethod {
    CAdES(null, MessageDigestAlgorithm.SHA256),
    XAdES(null, MessageDigestAlgorithm.SHA256),

    PEPPOL_V1(CAdES, MessageDigestAlgorithm.SHA256),
    SDP(XAdES, MessageDigestAlgorithm.SHA256),
    ;

    private SignatureMethod realSignatureMethod;
    private MessageDigestAlgorithm messageDigestAlgorithm;

    SignatureMethod(SignatureMethod realSignatureMethod, MessageDigestAlgorithm messageDigestAlgorithm) {
        this.realSignatureMethod = realSignatureMethod;
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public MessageDigestAlgorithm getMessageDigestAlgorithm() {
        return messageDigestAlgorithm;
    }

    public SignatureMethod getRealSignatureMethod() {
        return realSignatureMethod == null ? this : realSignatureMethod;
    }
}
