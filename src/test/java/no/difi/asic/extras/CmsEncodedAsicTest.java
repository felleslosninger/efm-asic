package no.difi.asic.extras;

import no.difi.asic.AsicReaderFactory;
import no.difi.asic.AsicWriterFactory;
import no.difi.asic.MimeType;
import no.difi.asic.SignatureHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class CmsEncodedAsicTest {

    @Test
    public void simple() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "changeit".toCharArray());

        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("selfsigned");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        CmsEncodedAsicWriter writer = new CmsEncodedAsicWriter(AsicWriterFactory.newFactory().newContainer(byteArrayOutputStream), certificate);
        writer.add(getClass().getResourceAsStream("/image.bmp"), "simple.bmp", MimeType.forString("image/bmp"));
        writer.addEncoded(getClass().getResourceAsStream("/image.bmp"), "encrypted.bmp", MimeType.forString("image/bmp"));
        writer.sign(new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", "selfsigned", "changeit"));

        PrivateKey privateKey = (PrivateKey) keyStore.getKey("selfsigned", "changeit".toCharArray());

        CmsEncodedAsicReader reader = new CmsEncodedAsicReader(AsicReaderFactory.newFactory().open(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())), privateKey);

        Assert.assertEquals(reader.getNextFile(), "simple.bmp");
        ByteArrayOutputStream file1 = new ByteArrayOutputStream();
        reader.writeFile(file1);

        Assert.assertEquals(reader.getNextFile(), "encrypted.bmp");
        ByteArrayOutputStream file2 = new ByteArrayOutputStream();
        reader.writeFile(file2);

        Assert.assertEquals(file2.toByteArray(), file1.toByteArray());

        Assert.assertNull(reader.getNextFile());

        Assert.assertEquals(reader.getAsicManifest().getCertificate().get(0).getCertificate(), certificate.getEncoded());
    }

}
