package no.difi.asic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.testng.Assert.*;

public class AsicUtilsTest {

    private static Logger log = LoggerFactory.getLogger(AsicUtilsTest.class);

    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();
    private SignatureHelper signatureHelper = new SignatureHelper(getClass().getResourceAsStream("/kontaktinfo-client-test.jks"), "changeit", null, "changeit");

    private String fileContent1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam arcu eros, fermentum vel molestie ut, sagittis vel velit.";
    private String fileContent2 = "Fusce eu risus ipsum. Sed mattis laoreet justo. Fusce nisi magna, posuere ac placerat tincidunt, dignissim non lacus.";

    @Test
    public void simpleCombine() throws IOException {
        // Create first container
        ByteArrayOutputStream source1 = new ByteArrayOutputStream();
        asicWriterFactory.newContainer(source1)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", MimeType.forString("text/plain"))
                .sign(signatureHelper);

        // Create second container
        ByteArrayOutputStream source2 = new ByteArrayOutputStream();
        AsicWriter asicWriter = asicWriterFactory.newContainer(source2)
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", MimeType.forString("text/plain"))
                // .add(new ByteArrayInputStream("manifest".getBytes()), "META-INF/manifest.xml", MimeType.forString("application/xml"))
                .sign(signatureHelper);

        // Test to make sure exception is thrown.
        try {
            asicWriter.getContainerFile();
            fail("Exception expected");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }

        // Combine containers
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        AsicUtils.combine(target, new ByteArrayInputStream(source1.toByteArray()), new ByteArrayInputStream(source2.toByteArray()));

        // Read container
        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(target.toByteArray()));

        ByteArrayOutputStream fileStream;
        {
            assertEquals(asicReader.getNextFile(), "content1.txt");

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileStream.toString(), fileContent1);
        }

        {
            assertEquals(asicReader.getNextFile(), "content2.txt");

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileStream.toString(), fileContent2);
        }

        assertNull(asicReader.getNextFile());

        asicReader.close();
    }

    // Making Cobertura happy!
    @Test
    public void constructor() {
        assertNotNull(new AsicUtils());
    }
}
