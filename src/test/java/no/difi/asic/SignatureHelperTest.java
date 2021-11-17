package no.difi.asic;

import static org.testng.Assert.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class SignatureHelperTest {

    private static Logger log = LoggerFactory.getLogger(SignatureHelperTest.class);

    @Test
    public void loadNoProblems() {
        new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", null, "changeit");
    }

    @Test
    public void loadNoProblemsWithKeyStore() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance(KeyStoreType.JKS.name());
        keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "changeit".toCharArray());

        new SignatureHelper(keyStore, null, "changeit");
    }


    @Test
    public void wrongKeystorePassword() {
        try {
            new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changed?", null, "changeit");
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test
    public void wrongKeyPassword() {
        try {
            new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", null, "changed?");
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }

    @Test
    public void wrongKeyAlias() {
        try {
            new SignatureHelper(getClass().getResourceAsStream("/keystore.jks"), "changeit", "asic", "changeit");
            fail("Exception expected.");
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        }
    }
}
