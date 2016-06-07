package no.difi.asic;

import org.testng.annotations.Test;

public class CadesAsicManifestTest {

    @Test(expectedExceptions = IllegalStateException.class)
    public void multipleRootFiles() {
        CadesAsicManifest manifest = new CadesAsicManifest(MessageDigestAlgorithm.SHA256);
        manifest.add("testfile1.xml", MimeType.XML);
        manifest.add("testfile2.xml", MimeType.XML);

        manifest.setRootfileForEntry("testfile1.xml");
        manifest.setRootfileForEntry("testfile2.xml");
    }
}
