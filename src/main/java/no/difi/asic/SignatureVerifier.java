package no.difi.asic;

import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author erlend
 */
public class SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SignatureHelper.class);

    private static JcaSimpleSignerInfoVerifierBuilder jcaSimpleSignerInfoVerifierBuilder =
            new JcaSimpleSignerInfoVerifierBuilder().setProvider(BCHelper.getProvider());

    @SuppressWarnings("unchecked")
    public final no.difi.commons.asic.jaxb.asic.Certificate validate(byte[] data, byte[] signature) {
        no.difi.commons.asic.jaxb.asic.Certificate certificate = null;

        try {
            CMSSignedData cmsSignedData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
            Store store = cmsSignedData.getCertificates();
            SignerInformationStore signerInformationStore = cmsSignedData.getSignerInfos();

            for (SignerInformation signerInformation : signerInformationStore.getSigners()) {
                X509CertificateHolder x509Certificate = (X509CertificateHolder) store.getMatches(signerInformation.getSID()).iterator().next();
                logger.info(x509Certificate.getSubject().toString());

                if (verifySigner(signerInformation) && signerInformation.verify(jcaSimpleSignerInfoVerifierBuilder.build(x509Certificate))) {
                    certificate = new no.difi.commons.asic.jaxb.asic.Certificate();
                    certificate.setCertificate(x509Certificate.getEncoded());
                    certificate.setSubject(x509Certificate.getSubject().toString());
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            certificate = null;
        }

        if (certificate == null)
            throw new IllegalStateException("Unable to verify signature.");

        return certificate;
    }

    protected boolean verifySigner(SignerInformation signerInformation) {
        // Example, extend as needed
        if(OIWObjectIdentifiers.md5WithRSA.equals(signerInformation.getDigestAlgorithmID())) {
            logger.warn("Signer algorithm "+signerInformation.getDigestAlgOID()+" is not allowed.");
            return false;
        }
        return true;
    }
}
