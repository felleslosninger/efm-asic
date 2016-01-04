package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.testng.Assert.*;

public class AsicReaderImplTest {

    private static Logger log = LoggerFactory.getLogger(AsicReaderImplTest.class);

    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();
    private SignatureHelper signatureHelper = new SignatureHelper(getClass().getResourceAsStream("/kontaktinfo-client-test.jks"), "changeit", null, "changeit");

    private String fileContent1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam arcu eros, fermentum vel molestie ut, sagittis vel velit.";
    private String fileContent2 = "Fusce eu risus ipsum. Sed mattis laoreet justo. Fusce nisi magna, posuere ac placerat tincidunt, dignissim non lacus.";

    @Test
    public void writeAndReadSimpleContainer() throws IOException {

        // Step 1 - creates the ASiC archive
        ByteArrayOutputStream containerOutput = new ByteArrayOutputStream();

        asicWriterFactory.newContainer(containerOutput)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", MimeType.forString("text/plain"))
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", MimeType.forString("text/plain"))
                .sign(signatureHelper);

        // Step 2 - reads the contents of the ASiC archive
        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(containerOutput.toByteArray()));

        ByteArrayOutputStream fileStream;
        {
            assertEquals("content1.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent1, fileStream.toString());
        }

        {
            assertEquals("content2.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent2, fileStream.toString());
        }

        assertNull(asicReader.getNextFile());

        try {
            asicReader.writeFile(new ByteArrayOutputStream());
            fail("Exception expected");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        asicReader.close();

        try {
            asicReader.close();
        } catch (IllegalStateException e) {
            fail("No exception expected");
        }

        assertEquals(asicReader.getAsicManifest().getFile().size(), 2);

    }


    @Test
    public void writeAndReadSimpleFileContainer() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        File file = new File(tmpDir, "asic-reader-sample.ip");

        asicWriterFactory.newContainer(file)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", MimeType.forString("text/plain"))
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", MimeType.forString("text/plain"))
                .sign(signatureHelper);

        AsicReader asicReader = asicReaderFactory.open(file);

        File contentFile;
        String filename;
        ByteArrayOutputStream fileStream;
        {
            filename = asicReader.getNextFile();
            assertEquals("content1.txt", filename);

            contentFile = new File(tmpDir, "asic-" + filename);
            asicReader.writeFile(contentFile);

            fileStream = new ByteArrayOutputStream();
            ByteStreams.copy(Files.newInputStream(contentFile.toPath()), fileStream);
            assertEquals(fileContent1, fileStream.toString());

            Files.delete(contentFile.toPath());
        }

        {
            filename = asicReader.getNextFile();
            assertEquals("content2.txt", filename);

            contentFile = new File(tmpDir, "asic-" + filename);
            asicReader.writeFile(contentFile);

            fileStream = new ByteArrayOutputStream();
            ByteStreams.copy(Files.newInputStream(contentFile.toPath()), fileStream);
            assertEquals(fileContent2, fileStream.toString());

            Files.delete(contentFile.toPath());
        }

        assertNull(asicReader.getNextFile());

        try {
            asicReader.writeFile(new ByteArrayOutputStream());
            fail("Exception expected");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        asicReader.close();

        try {
            asicReader.close();
        } catch (IllegalStateException e) {
            fail("No exception expected");
        }

        Files.delete(file.toPath());
    }

    @Test
    public void exceptionOnInvalidMime() throws IOException {
        AsicReader asicReader = asicReaderFactory.open(getClass().getResourceAsStream("/asic-general-test-invalid-mime.asice"));

        try {
            asicReader.getNextFile();
            fail("Didn't throw exception on wrong mimetype.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        asicReader.close();

        try {
            asicReader.close();
        } catch (IllegalStateException e) {
            fail("No exception expected");
        }

    }
}
