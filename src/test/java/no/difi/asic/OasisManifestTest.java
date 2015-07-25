package no.difi.asic;

import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;

public class OasisManifestTest {

    private static Logger log = LoggerFactory.getLogger(OasisManifestTest.class);

    @Test
    public void simpleTest() {
        OasisManifest oasisManifest = new OasisManifest(MimeType.forString(AsicUtils.MIMETYPE_ASICE));
        oasisManifest.add("test.xml", MimeType.forString("application/text"));

        log.info(new String(oasisManifest.toBytes()));
    }

    @Test
    public void triggerReadException() {
        try {
            new OasisManifest(new ByteArrayInputStream("invalid data".getBytes()));
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

}
