package no.difi.asic;

import no.difi.xsd.asic.model._1.AsicManifest;
import org.etsi.uri._02918.v1_2.DataObjectReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
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
public class AsicWriterTest {

    public static final Logger log = LoggerFactory.getLogger(AsicWriterTest.class);

    public static final String BII_ENVELOPE_XML = "bii-envelope.xml";
    public static final String BII_MESSAGE_XML = TestUtil.BII_SAMPLE_MESSAGE_XML;
    private File keystoreFile;

    private AsicVerifierFactory asicVerifierFactory;
    private File biiEnvelopeFile;
    private File biiMessageFile;

    @BeforeMethod
    public void setUp() {
        try {
            URL envelopeUrl = AsicWriterTest.class.getClassLoader().getResource(BII_ENVELOPE_XML);
            assertNotNull(envelopeUrl);

            biiEnvelopeFile = new File(envelopeUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to convert resource " + BII_ENVELOPE_XML + " into a File object using URIs:" + e.getMessage(), e);
        }


        try {
            URL messageUrl;
            messageUrl = AsicWriterTest.class.getClassLoader().getResource(BII_MESSAGE_XML);
            assertNotNull(messageUrl);
            biiMessageFile = new File(messageUrl.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        keystoreFile = TestUtil.keyStoreFile();
        assertTrue(keystoreFile.canRead(), "Expected to find your private key and certificate in " + keystoreFile);

        asicVerifierFactory = AsicVerifierFactory.newFactory(); // Assumes default signature method
    }


    @Test
    public void createSampleContainer() throws Exception {

        // PART 1 - creates the ASiC archive

        // Name of the file to hold the the ASiC archive
        File archiveOutputFile = new File(System.getProperty("java.io.tmpdir"), "asic-sample-default.zip");

        // Creates an AsicWriterFactory with default signature method
        AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

        // Creates the actual container with all the data objects (files) and signs it.
        AsicWriter asicWriter = asicWriterFactory.newContainer(archiveOutputFile)
                // Adds an ordinary file, using the file name as the entry name
                .add(biiEnvelopeFile)
                        // Adds another file, explicitly naming the entry and specifying the MIME type
                .add(biiMessageFile, BII_MESSAGE_XML, MimeType.forString("application/xml"))
                        // Indicates that the BII message is the root document
                .setRootEntryName(BII_MESSAGE_XML)
                        // Signing the contents of the archive, closes it for further changes.
                .sign(keystoreFile, TestUtil.keyStorePassword(), TestUtil.privateKeyPassword());


        // PART 2 - verify the contents of the archive.

        {
            int matchCount = 0;
            CadesAsicManifest asicManifest = (CadesAsicManifest) ((CadesAsicWriter) asicWriter).getAsicManifest();
            for (DataObjectReference dataObject : asicManifest.getASiCManifestType().getDataObjectReferences()) {
                if (dataObject.getURI().equals(BII_ENVELOPE_XML))
                    matchCount++;
                if (dataObject.getURI().equals(BII_MESSAGE_XML))
                    matchCount++;
            }
            assertEquals(matchCount, 2, "Entries were not added properly into list");
        }

        assertTrue(archiveOutputFile.canRead(), "ASiC container can not be read");

        log.info("Generated file " + archiveOutputFile);

        ZipFile zipFile = new ZipFile(archiveOutputFile);
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
            asicWriter.add(biiEnvelopeFile);
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

        AsicVerifier asicVerifier = asicVerifierFactory.verify(archiveOutputFile);
        assertEquals(asicVerifier.getAsicManifest().getFiles().size(), 2);
    }

    @Test
    public void writeAndRead() throws Exception {
        // Name of the file to hold the the ASiC archive
        File archiveOutputFile = new File(System.getProperty("java.io.tmpdir"), "asic-sample-default.zip");

        // Creates an AsicWriterFactory with default signature method
        AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

        // Creates the actual container with all the data objects (files) and signs it.
        asicWriterFactory.newContainer(archiveOutputFile)
                // Adds an ordinary file, using the file name as the entry name
                .add(biiEnvelopeFile)
                        // Adds another file, explicitly naming the entry and specifying the MIME type
                .add(biiMessageFile, BII_MESSAGE_XML, MimeType.forString("application/xml"))
                // Indicates which file is the root file
                .setRootEntryName(BII_MESSAGE_XML)
                        // Signing the contents of the archive, closes it for further changes.
                .sign(keystoreFile, TestUtil.keyStorePassword(), TestUtil.privateKeyPassword());

        log.debug("Wrote ASiC-e container to " + archiveOutputFile);
        // Opens the generated archive and reads each entry
        AsicReader asicReader = AsicReaderFactory.newFactory().open(archiveOutputFile);

        String entryName;

        // Iterates over each entry and writes the contents into a file having same name as the entry
        while ((entryName = asicReader.getNextFile()) != null) {
            log.debug("Read entry " + entryName);

            // Creates file with same name as entry
            File file = new File(entryName);
            // Ensures we don't overwrite anything
            if (file.exists()) {
                throw new IllegalStateException("File already exists");
            }
            asicReader.writeFile(file);

            // Removes file immediately, since this is just a test
            file.delete();
        }
        asicReader.close();
        AsicManifest asicManifest = asicReader.getAsicManifest();
        String asicManifestRootfile = asicManifest.getRootfile();
        assertNotNull(asicManifestRootfile,"Root file not found");
        assertEquals(asicManifestRootfile,BII_MESSAGE_XML,"Invalid Rootfile found");



    }

    @Test
    public void unknownMimetype() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();
            asicWriterFactory.newContainer(byteArrayOutputStream)
                    .add(biiEnvelopeFile, "envelope.aaz");
            fail("Expected exception, is .aaz a known extension?");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

    }
}
