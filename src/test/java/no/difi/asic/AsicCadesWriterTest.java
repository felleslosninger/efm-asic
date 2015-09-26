package no.difi.asic;

import org.etsi.uri._02918.v1_2.DataObjectReferenceType;
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
public class AsicCadesWriterTest {

    public static final Logger log = LoggerFactory.getLogger(AsicCadesWriterTest.class);

    public static final int BYTES_TO_CHECK = 40;
    public static final String BII_ENVELOPE_XML = "bii-envelope.xml";
    public static final String BII_MESSAGE_XML = TestUtil.BII_SAMPLE_MESSAGE_XML;
    private URL envelopeUrl;
    private URL messageUrl;
    private File keystoreFile;

    private AsicWriterFactory asicWriterFactory;
    private AsicVerifierFactory asicVerifierFactory;

    @BeforeMethod
    public void setUp() {
        envelopeUrl = AsicCadesWriterTest.class.getClassLoader().getResource(BII_ENVELOPE_XML);
        assertNotNull(envelopeUrl);

        messageUrl = AsicCadesWriterTest.class.getClassLoader().getResource(BII_MESSAGE_XML);
        assertNotNull(messageUrl);

        keystoreFile = TestUtil.keyStoreFile();
        assertTrue(keystoreFile.canRead(), "Expected to find your private key and certificate in " + keystoreFile);

        asicWriterFactory = AsicWriterFactory.newFactory();
        asicVerifierFactory = AsicVerifierFactory.newFactory();
    }

    @Test
    public void createSampleEmptyContainer() throws Exception {

        File file = new File(System.getProperty("java.io.tmpdir"), "asic-empty-sample-cades.zip");

        asicWriterFactory.newContainer(file).sign(keystoreFile, TestUtil.keyStorePassword(), TestUtil.privateKeyPassword());

        assertTrue(file.exists() && file.isFile() && file.canRead(), file + " can not be read");

        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream is = new BufferedInputStream(fileInputStream);

        byte[] buffer = new byte[BYTES_TO_CHECK];
        int read = is.read(buffer, 0, BYTES_TO_CHECK);
        assertEquals(read, BYTES_TO_CHECK);

        assertEquals(buffer[28], (byte) 0, "Byte 28 should be 0");

        assertEquals(buffer[8], 0, "'mimetype' file should not be compressed");

        assertTrue(buffer[0] == 0x50 && buffer[1] == 0x4B && buffer[2] == 0x03 && buffer[3] == 0x04, "First 4 octets should read 0x50 0x4B 0x03 0x04");
    }

    @Test
    public void createSampleContainer() throws Exception {

        File asicOutputFile = new File(System.getProperty("java.io.tmpdir"), "asic-sample-cades.zip");

        AsicWriter asicWriter = asicWriterFactory.newContainer(asicOutputFile)
                .add(new File(envelopeUrl.toURI()))
                // Specifies the file, the archive entry name and explicitly names the MIME type
                .add(new File(messageUrl.toURI()), BII_MESSAGE_XML, MimeType.forString("application/xml"))
                .setRootEntryName(envelopeUrl.toURI().toString())
                .sign(keystoreFile, TestUtil.keyStorePassword(), TestUtil.keyPairAlias(), TestUtil.privateKeyPassword());

        // Verifies that both files have been added.
        {
            int matchCount = 0;
            CadesAsicManifest asicManifest = (CadesAsicManifest) ((CadesAsicWriter) asicWriter).getAsicManifest();
            for (DataObjectReferenceType dataObject : asicManifest.getASiCManifestType().getDataObjectReference()) {
                if (dataObject.getURI().equals(BII_ENVELOPE_XML))
                    matchCount++;
                if (dataObject.getURI().equals(BII_MESSAGE_XML))
                    matchCount++;
            }
            assertEquals(matchCount, 2, "Entries were not added properly into list");
        }

        assertTrue(asicOutputFile.canRead(), "ASiC container can not be read");

        log.info("Generated file " + asicOutputFile);

        ZipFile zipFile = new ZipFile(asicOutputFile);
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
            asicWriter.sign(new SignatureHelper(keystoreFile, TestUtil.keyStorePassword(), TestUtil.privateKeyPassword()));
            fail("Exception expected");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        asicVerifierFactory.verify(asicOutputFile);
    }

    @Test
    public void writingToMetaInf() throws IOException {
        AsicWriter asicWriter = asicWriterFactory.newContainer(new ByteArrayOutputStream());

        try {
            asicWriter.add(new ByteArrayInputStream("Demo".getBytes()), "META-INF/demo.xml");
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.debug(e.getMessage());
        }
    }
}
