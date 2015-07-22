package no.difi.asic;


import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class AsicUtilsTest {

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
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", "text/plain")
                .sign(signatureHelper);

        // Create second container
        ByteArrayOutputStream source2 = new ByteArrayOutputStream();
        asicWriterFactory.newContainer(source2)
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", "text/plain")
                .sign(signatureHelper);

        // Combine containers
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        AsicUtils.combine(target, new ByteArrayInputStream(source1.toByteArray()), new ByteArrayInputStream(source2.toByteArray()));

        // Read container
        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(target.toByteArray()));

        ByteArrayOutputStream fileStream;
        {
            assertEquals("content1.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent1, fileStream.toString());
        }

        // To be removed at a later state.
        assertEquals("META-INF/asicmanifest1.xml", asicReader.getNextFile());
        asicReader.getNextFile(); // Signature file

        {
            assertEquals("content2.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent2, fileStream.toString());
        }

        // To be removed at a later state.
        assertEquals("META-INF/asicmanifest2.xml", asicReader.getNextFile());
        asicReader.getNextFile(); // Signature file

        assertNull(asicReader.getNextFile());

        asicReader.close();
    }

}
