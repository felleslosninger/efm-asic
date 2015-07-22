package no.difi.asic;

import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AsicReaderTest {

    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

    private SignatureHelper signatureHelper;

    @BeforeClass
    public void beforeClass() {
        signatureHelper = new SignatureHelper(AsicReaderTest.class.getResourceAsStream("/kontaktinfo-client-test.jks"), "changeit", null, "changeit");
    }

    @Test
    public void WriteAndReadSimpleContainer() throws IOException {
        String fileContent1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam arcu eros, fermentum vel molestie ut, sagittis vel velit.";
        String fileContent2 = "Fusce eu risus ipsum. Sed mattis laoreet justo. Fusce nisi magna, posuere ac placerat tincidunt, dignissim non lacus.";

        ByteArrayOutputStream containerOutput = new ByteArrayOutputStream();

        asicWriterFactory.newContainer(containerOutput)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", "text/plain")
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", "text/plain")
                .sign(signatureHelper);

        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(containerOutput.toByteArray()));

        assertEquals("content1.txt", asicReader.getNextFile());

        ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
        asicReader.writeFile(fileStream);
        assertEquals(fileContent1, fileStream.toString());

        assertEquals("content2.txt", asicReader.getNextFile());

        fileStream = new ByteArrayOutputStream();
        asicReader.writeFile(fileStream);
        assertEquals(fileContent2, fileStream.toString());

        // To be removed at a later state.
        assertEquals("META-INF/asicmanifest.xml", asicReader.getNextFile());
        assertEquals("META-INF/signature.p7s", asicReader.getNextFile());

        assertNull(asicReader.getNextFile());

        asicReader.close();
    }

}
