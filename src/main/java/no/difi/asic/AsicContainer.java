package no.difi.asic;

import org.etsi.uri._2918.v1_1.ASiCManifestType;
import org.etsi.uri._2918.v1_1.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author steinar
 *         Date: 02.07.15
 *         Time: 12.44
 */
public class AsicContainer {

    public static final Logger log = LoggerFactory.getLogger(AsicContainer.class);

    /**
     * Holds the file reference for the ASiC container
     */
    private File file;

    /**
     * Message digester used for calculating digests.
     */

    public File getFile() {
        return file;
    }

    public AsicContainer(File file) {
        this.file = file;
    }


}
