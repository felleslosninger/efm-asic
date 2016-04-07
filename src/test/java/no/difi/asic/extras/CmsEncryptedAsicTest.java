package no.difi.asic.extras;

import no.difi.asic.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CmsEncryptedAsicTest {

    @Test
    public void simple() throws Exception {

        // WRITE TO ASIC

        // Read JKS
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "changeit".toCharArray());

        // Fetching certificate
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("selfsigned");

        // Store result in ByteArrayOutputStream
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Create a new ASiC archive
        AsicWriter asicWriter = AsicWriterFactory.newFactory().newContainer(byteArrayOutputStream);
        // Encapsulate ASiC archive to enable writing encrypted content
        CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(asicWriter, certificate);
        writer.add(getClass().getResourceAsStream("/image.bmp"), "simple.bmp", MimeType.forString("image/bmp"));
        writer.addEncrypted(getClass().getResourceAsStream("/image.bmp"), "encrypted.bmp", MimeType.forString("image/bmp"));
        writer.sign(new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", "selfsigned", "changeit"));
        // ByteArrayOutputStream now contains a signed ASiC archive containing one encrypted file


        // READ FROM ASIC

        // Fetch private key from keystore
        PrivateKey privateKey = (PrivateKey) keyStore.getKey("selfsigned", "changeit".toCharArray());

        // Open content of ByteArrayOutputStream for reading
        AsicReader asicReader = AsicReaderFactory.newFactory().open(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        // Encapsulate ASiC archive to enable reading encrypted content
        CmsEncryptedAsicReader reader = new CmsEncryptedAsicReader(asicReader, privateKey);

        // Read plain file
        Assert.assertEquals(reader.getNextFile(), "simple.bmp");
        ByteArrayOutputStream file1 = new ByteArrayOutputStream();
        reader.writeFile(file1);

        // Read encrypted file
        Assert.assertEquals(reader.getNextFile(), "encrypted.bmp");
        ByteArrayOutputStream file2 = new ByteArrayOutputStream();
        reader.writeFile(file2);

        // Verify both files contain the same data
        Assert.assertEquals(file2.toByteArray(), file1.toByteArray());

        // Verify no more files are found
        Assert.assertNull(reader.getNextFile());

        // Verify certificate used for signing of ASiC is the same as the one used for signing
        Assert.assertEquals(reader.getAsicManifest().getCertificate().get(0).getCertificate(), certificate.getEncoded());
    }

}
