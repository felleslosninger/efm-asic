package no.difi.asic;

enum MessageDigestAlgorithm {
    SHA256("SHA-256", "http://www.w3.org/2001/04/xmlenc#sha256"),
    SHA384("SHA-384", "http://www.w3.org/2000/09/xmldsig#sha384"),
    SHA512("SHA-512", "http://www.w3.org/2000/09/xmldsig#sha512");

    private String algorithm;
    private String uri;

    MessageDigestAlgorithm(String algorithm, String uri) {
        this.algorithm = algorithm;
        this.uri = uri;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getUri() {
        return uri;
    }
}
