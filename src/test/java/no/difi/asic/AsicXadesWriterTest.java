package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.testng.Assert.*;

/**
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.08
 */
public class AsicXadesWriterTest {

    public static final Logger log = LoggerFactory.getLogger(AsicXadesWriterTest.class);

    public static final String BII_ENVELOPE_XML = "bii-envelope.xml";
    public static final String BII_MESSAGE_XML = "bii-message.xml";
    private URL envelopeUrl;
    private URL messageUrl;
    private File keystoreFile;

    private AsicWriterFactory asicContainerWriterFactory;

    @BeforeMethod
    public void setUp() {
        envelopeUrl = AsicXadesWriterTest.class.getClassLoader().getResource(BII_ENVELOPE_XML);
        assertNotNull(envelopeUrl);

        messageUrl = AsicXadesWriterTest.class.getClassLoader().getResource(BII_MESSAGE_XML);
        assertNotNull(messageUrl);

        keystoreFile = new File("src/test/resources/kontaktinfo-client-test.jks");
        assertTrue(keystoreFile.canRead(), "Expected to find your private key and certificate in " + keystoreFile);

        asicContainerWriterFactory = AsicWriterFactory.newFactory(SignatureMethod.XAdES);
    }

    @Test
    public void createSampleEmptyContainer() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        asicContainerWriterFactory.newContainer(outputStream).sign(keystoreFile, "changeit", "changeit");

        byte[] buffer = outputStream.toByteArray();
        assertEquals(buffer[28], (byte) 0, "Byte 28 should be 0");
        assertEquals(buffer[8], 0, "'mimetype' file should not be compressed");
        assertTrue(buffer[0] == 0x50 && buffer[1] == 0x4B && buffer[2] == 0x03 && buffer[3] == 0x04, "First 4 octets should read 0x50 0x4B 0x03 0x04");
    }

    @Test
    public void createSampleContainer() throws Exception {

        AsicWriter asicWriter = asicContainerWriterFactory.newContainer(new File(System.getProperty("java.io.tmpdir")), "asic-sample-xades.zip")
                .add(new File(envelopeUrl.toURI()))
                .add(new File(messageUrl.toURI()), "bii-message.xml", "application/xml")
                .sign(keystoreFile, "changeit", "client_alias", "changeit");

        File file = asicWriter.getContainerFile();

        // Verifies that both files have been added.
        {
            int matchCount = 0;
            XadesAsicManifest asicManifest = (XadesAsicManifest) ((XadesAsicWriter) asicWriter).getAsicManifest();
            /*
            for (DataObjectReferenceType dataObject : asicManifest.getASiCManifestType().getDataObjectReference()) {
                if (dataObject.getURI().equals(BII_ENVELOPE_XML))
                    matchCount++;
                if (dataObject.getURI().equals(BII_MESSAGE_XML))
                    matchCount++;
            }
            */
            // assertEquals(matchCount, 2, "Entries were not added properly into list");
        }

        assertTrue(file.canRead(), "ASiC container can not be read");

        log.info("Generated file " + file);

        ZipFile zipFile = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        {
            int matchCount = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (BII_ENVELOPE_XML.equals(name)) {
                    matchCount++;
                }
                if (BII_MESSAGE_XML.equals(name)) {
                    matchCount++;
                }
                log.info("Found " + name);
                InputStream stream = zipFile.getInputStream(entry);
            }
            assertEquals(matchCount, 2, "Number of items in archive did not match");
        }

        try {
            asicWriter.add(new File(envelopeUrl.toURI()));
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        try {
            asicWriter.sign(keystoreFile, "changeit", "changeit");
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }
    }
}
