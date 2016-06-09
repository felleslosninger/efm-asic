package no.difi.asic.extras;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

abstract class CmsEncryptedAsicAbstract {

    protected static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(new BouncyCastleProvider());
    }
}
