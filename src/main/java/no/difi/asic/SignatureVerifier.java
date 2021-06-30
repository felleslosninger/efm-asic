package no.difi.asic;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author erlend
 */
public class SignatureVerifier {

    private static final Logger logger = LoggerFactory.getLogger(SignatureHelper.class);

    private static final JcaSimpleSignerInfoVerifierBuilder jcaSimpleSignerInfoVerifierBuilder =
            new JcaSimpleSignerInfoVerifierBuilder().setProvider(BCHelper.getProvider());

    @SuppressWarnings("unchecked")
    public static List<no.difi.commons.asic.jaxb.asic.Certificate> validate(byte[] data, byte[] signature) {
        List<X509Certificate> x509CertificateChain = new ArrayList<>();
        List<no.difi.commons.asic.jaxb.asic.Certificate> certificateChain = new ArrayList<>();
        no.difi.commons.asic.jaxb.asic.Certificate signerCertificate = null;

        try {
            CMSSignedData cmsSignedData = new CMSSignedData(new CMSProcessableByteArray(data), signature);
            Store store = cmsSignedData.getCertificates();
            SignerInformationStore signerInformationStore = cmsSignedData.getSignerInfos();

            // Extract and verify the signing certificate
            for (SignerInformation signerInformation : signerInformationStore.getSigners()) {
                X509CertificateHolder x509Certificate = (X509CertificateHolder) store.getMatches(signerInformation.getSID()).iterator().next();
                logger.info(x509Certificate.getSubject().toString());

                if (signerInformation.verify(jcaSimpleSignerInfoVerifierBuilder.build(x509Certificate))) {
                    x509CertificateChain.add(new JcaX509CertificateConverter().getCertificate(x509Certificate));
                    signerCertificate = new no.difi.commons.asic.jaxb.asic.Certificate();
                    signerCertificate.setCertificate(x509Certificate.getEncoded());
                    signerCertificate.setSubject(x509Certificate.getSubject().toString());
                    certificateChain.add(signerCertificate);
                    break; //Since a single signer is assumed throughout this program, break when one is found
                }
            }

            // Build the certificate chain, verifying the issuer's signature for each certificate in the chain
            Collection<X509CertificateHolder> allCerts = store.getMatches(null);
            for(int i =0; i<allCerts.size()-1; i++){
                for (X509CertificateHolder holder : allCerts){
                    X509Certificate lastCert = x509CertificateChain.get(x509CertificateChain.size()-1);
                    X509Certificate issuer = new JcaX509CertificateConverter().getCertificate(holder);
                    if(issuer.getSubjectDN().equals(lastCert.getIssuerDN())){
                        lastCert.verify(issuer.getPublicKey());
                        x509CertificateChain.add(issuer);
                        no.difi.commons.asic.jaxb.asic.Certificate issuerCertificate = new no.difi.commons.asic.jaxb.asic.Certificate();
                        issuerCertificate.setCertificate(issuer.getEncoded());
                        issuerCertificate.setSubject(issuer.getSubjectDN().getName());
                        certificateChain.add(issuerCertificate);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
            signerCertificate = null;
        }

        if (signerCertificate == null)
            throw new IllegalStateException("Unable to verify signature.");

        return certificateChain;
    }
}
