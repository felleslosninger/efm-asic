package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.08
 */
public class AsicBuilderTest {

    public static final Logger log = LoggerFactory.getLogger(AsicBuilderTest.class);

    public static final int BYTES_TO_CHECK = 40;
    public static final String BII_ENVELOPE_XML = "bii-envelope.xml";
    public static final String BII_MESSAGE_XML = "bii-message.xml";
    private URL envelopeUrl;
    private URL messageUrl;
    private File keystoreFile;

    @BeforeMethod
    public void setUp() {
        envelopeUrl = AsicBuilderTest.class.getClassLoader().getResource(BII_ENVELOPE_XML);
        assertNotNull(envelopeUrl);

        messageUrl = AsicBuilderTest.class.getClassLoader().getResource(BII_MESSAGE_XML);
        assertNotNull(messageUrl);

        String homeDirName = System.getProperty("user.home");
        File homeDir = new File(homeDirName);
        assertTrue(homeDir.isDirectory() && homeDir.canRead(), homeDirName + " does not exist or is not readable");

        String keyStoreFileName = ".ssh/comodo.jks";
        keystoreFile = new File(homeDir, keyStoreFileName);
        assertTrue(keystoreFile.canRead(), "Expected to find your private key and certificate in " + keystoreFile);

    }


    @Test
    public void createSampleEmptyContainer() throws Exception {

        AsicBuilder asicBuilder = new AsicBuilder()
                .archiveName("asic-test")
                .outputDirectory(new File(System.getProperty("java.io.tmpdir")))
                .keyStore(keystoreFile)
                .keyStorePassword("ringo1")
                .privateKeyPassword("ringo1");

        AsicContainer asicContainer = asicBuilder.build();

        File asicFile = asicContainer.getFile();

        assertTrue(asicFile.exists() && asicFile.isFile() && asicFile.canRead(), asicFile + " can not be read");

        long fileSize = asicFile.length();

        FileInputStream fileInputStream = new FileInputStream(asicFile);
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


        AsicBuilder asicBuilder = new AsicBuilder();

        // Creates the ASiC container in whatever is the temporary directory
        asicBuilder.outputDirectory(new File(System.getProperty("java.io.tmpdir")));

        // TODO: Verify the various rules for the file name extension
        // Name of file holding the ASiC container (the zip file)
        asicBuilder.archiveName("asic-sample.zip"); // This will override the default extension of .asice or .sce

        File fileReference = new File(envelopeUrl.toURI());

        asicBuilder.addFile(fileReference, fileReference.getName())
                .addFile(new File(messageUrl.toURI()))
                .keyStore(keystoreFile)
                .keyStorePassword("ringo1")
                .privateKeyPassword("ringo1");

        // Verifies that both files have been added.
        {
            int matchCount = 0;
            for (Map.Entry<String, AsicDataObjectEntry> entry : asicBuilder.getFiles().entrySet()) {
                if (entry.getKey().equals(BII_ENVELOPE_XML)) {
                    matchCount++;
                }
                if (entry.getKey().equals(BII_MESSAGE_XML)) {
                    matchCount++;
                }
            }
            assertEquals(matchCount, 2, "Entries were not added properly into list");
        }

        AsicContainer asicContainer = asicBuilder.build();


        assertNotNull(asicContainer.getFile());
        assertTrue(asicContainer.getFile().canRead(), "ASiC container can not be read");

        log.info("Generated file " + asicContainer.getFile());

        ZipFile zipFile = new ZipFile(asicContainer.getFile());
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
    }

}
