package no.difi.asic;

import no.difi.xsd.asic.model._1.AsicFile;
import no.difi.xsd.asic.model._1.AsicManifest;

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

    public void update(String filename, byte[] digest) {
        update(filename, null, digest, null);
    }

    public void update(String filename, String mimetype, byte[] digest, String digestAlgorithm) {
        if (digestAlgorithm != null && !digestAlgorithm.equals(messageDigestAlgorithm.getUri()))
            throw new IllegalStateException(String.format("Wrong digest method for file %s: %s", filename, digestAlgorithm));

        AsicFile asicFile = asicManifestMap.get(filename);

        if (asicFile == null) {
            asicFile = new AsicFile();
            asicFile.setName(filename);
            asicFile.setMimetype(mimetype);
            asicFile.setDigest(digest);
            asicFile.setVerified(false);

            asicManifest.getFile().add(asicFile);
            asicManifestMap.put(filename, asicFile);
        } else {
            if (mimetype != null)
                asicFile.setMimetype(mimetype);

            if (!Arrays.equals(asicFile.getDigest(), digest))
                throw new IllegalStateException(String.format("Mismatching digest for file %s", filename));

            asicFile.setVerified(true);
        }
    }

    public void verifyAllVerified() {
        for (AsicFile asicFile : asicManifest.getFile())
            if (!asicFile.isVerified())
                throw new IllegalStateException(String.format("File not verified: %s", asicFile.getName()));
    }

    public AsicManifest getAsicManifest() {
        return asicManifest;
    }
}
