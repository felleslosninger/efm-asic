package no.difi.asic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.13
 */
public class ZipFileCreatorTest {

    public static final Logger log = LoggerFactory.getLogger(ZipFileCreatorTest.class);

    @Test
    public void createZipFile() throws Exception {

        String xmlData = "<manifest>test</manifest>";

        String defaultTempDirName = System.getProperty("java.io.tmpdir");
        File tempDirRef = new File(defaultTempDirName);

        File zipFileRef = File.createTempFile("Zipest", ".zip", tempDirRef);

        FileOutputStream fileOutputStream = new FileOutputStream(zipFileRef);
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

/*
        asicOutputStream.putNextEntry(new ZipEntry("META-INF/"));
*/
        zipOutputStream.putNextEntry(new ZipEntry("META-INF/manifest.xml"));

        zipOutputStream.write(xmlData.getBytes());

        zipOutputStream.closeEntry();

        zipOutputStream.close();

        log.info("Created file " + zipFileRef);


    }

    @Test
    public void paths() throws Exception {

        File currentWorkingDirectory = new File(".");

        File fileInCWD = new File("META-INF/ole.txt");
        boolean absolute = fileInCWD.isAbsolute();
        assertFalse(absolute);

        String fileName = fileInCWD.toString();
        String s2 = fileInCWD.getAbsolutePath();

        URI fileUri = fileInCWD.toURI();
        assertTrue(fileUri.isAbsolute());

        URI cwdUri = currentWorkingDirectory.toURI();

        // Makes the URI of the file in CWD into a relative URI
        URI relativize = cwdUri.relativize(fileUri);

        boolean absolute2 = relativize.isAbsolute();
        assertFalse(absolute2)
        ;
    }

    @Test
    public void verifyRelativeURI() throws Exception {
        URI uri = new URI("META-INF/asicmanifest.xml");
        String s = uri.toASCIIString();
        assertEquals(s, "META-INF/asicmanifest.xml");

    }
}
