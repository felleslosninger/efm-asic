package no.difi.asic.extras;

import no.difi.asic.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CmsEncryptedAsicTest {

    @Test
    public void simple() throws Exception {

        // WRITE TO ASIC
        KeyStore keyStore = loadKeyStore();

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

        // Writes the ASiC file to temporary directory
        File sample = File.createTempFile("sample", ".asice");
        try (FileOutputStream fileOutputStream = new FileOutputStream(sample);) {
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
        }
        System.out.println("Wrote sample ASiC to " + sample);

    }

    private KeyStore loadKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // Read JKS
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "changeit".toCharArray());
        return keyStore;
    }

    @Test
    public void createSampleForBits() throws Exception {

        // Obtains the keystore
        KeyStore keyStore = loadKeyStore();

        // Fetching certificate
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("selfsigned");

        // Store result in outputfile
        File sample = File.createTempFile("sample-bits", ".asice");
        try (FileOutputStream fileOutputStream = new FileOutputStream(sample);) {

            // Create a new ASiC archive
            AsicWriter asicWriter = AsicWriterFactory.newFactory().newContainer(fileOutputStream);
            // Encapsulate ASiC archive to enable writing encrypted content
            CmsEncryptedAsicWriter writer = new CmsEncryptedAsicWriter(asicWriter, certificate);

            // Adds the SBDH
            writer.add(getClass().getResourceAsStream("/sample-sbdh.xml"), "sbdh.xml", MimeType.forString("application/xml"));

            // Adds the plain text sample document
            writer.add(getClass().getResourceAsStream("/bii-trns081.xml"), "sample.xml", MimeType.forString("application/xml"));

            // Adds the encrypted version of the sample document
            writer.addEncrypted(getClass().getResourceAsStream("/bii-trns081.xml"), "sample.xml", MimeType.forString("application/xml"));

            // Indicates which document is the root entry (to be read first)
            writer.setRootEntryName("sample.xml");

            // Signs the archive
            SignatureHelper signatureHelper = new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", "selfsigned", "changeit");
            writer.sign(signatureHelper);

        }

        System.out.println("Wrote sample ASiC to " + sample);
    }

}
