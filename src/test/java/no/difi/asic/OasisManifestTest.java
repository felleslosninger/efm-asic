package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class OasisManifestTest {

    private static Logger log = LoggerFactory.getLogger(OasisManifestTest.class);

    @Test
    public void simpleTest() {
        OasisManifest oasisManifest = new OasisManifest(MimeType.forString(AbstractAsicWriter.APPLICATION_VND_ETSI_ASIC_E_ZIP));
        oasisManifest.add("test.xml", MimeType.forString("application/text"));

        log.info(new String(oasisManifest.toBytes()));
    }

}
