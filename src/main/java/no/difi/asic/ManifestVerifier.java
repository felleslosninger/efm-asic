package no.difi.asic;

import no.difi.xsd.asic.model._1.AsicFile;
import no.difi.xsd.asic.model._1.AsicManifest;
import no.difi.xsd.asic.model._1.Certificate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class ManifestVerifier {

    private MessageDigestAlgorithm messageDigestAlgorithm;

    private AsicManifest asicManifest = new AsicManifest();
    private Map<String, AsicFile> asicManifestMap = new HashMap<>();

    public ManifestVerifier(MessageDigestAlgorithm messageDigestAlgorithm) {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public void update(String filename, byte[] digest, String sigReference) {
        update(filename, null, digest, null, sigReference);
    }

    public void update(String filename, String mimetype, byte[] digest, String digestAlgorithm, String sigReference) {
        if (digestAlgorithm != null && !digestAlgorithm.equals(messageDigestAlgorithm.getUri()))
            throw new IllegalStateException(String.format("Wrong digest method for file %s: %s", filename, digestAlgorithm));

        AsicFile asicFile = asicManifestMap.get(filename);

        if (asicFile == null) {
            asicFile = new AsicFile();
            asicFile.setName(filename);
            asicFile.setDigest(digest);
            asicFile.setVerified(false);

            asicManifest.getFiles().add(asicFile);
            asicManifestMap.put(filename, asicFile);
        } else {
            if (!Arrays.equals(asicFile.getDigest(), digest))
                throw new IllegalStateException(String.format("Mismatching digest for file %s", filename));

            asicFile.setVerified(true);
        }

        if (mimetype != null)
            asicFile.setMimetype(mimetype);
        if (sigReference != null)
            asicFile.getCertReves().add(sigReference);

    }

    public void addCertificate(Certificate certificate) {
        this.asicManifest.getCertificates().add(certificate);
    }

    public void verifyAllVerified() {
        for (AsicFile asicFile : asicManifest.getFiles())
            if (!asicFile.isVerified())
                throw new IllegalStateException(String.format("File not verified: %s", asicFile.getName()));
    }

    public AsicManifest getAsicManifest() {
        return asicManifest;
    }
}
