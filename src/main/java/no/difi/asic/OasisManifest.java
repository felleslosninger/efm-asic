package no.difi.asic;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import no.difi.commons.asic.jaxb.opendocument.manifest.FileEntry;
import no.difi.commons.asic.jaxb.opendocument.manifest.Manifest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

class OasisManifest {

    private static JAXBContext jaxbContext; // Thread safe

    static {
        try {
            jaxbContext = JAXBContext.newInstance(Manifest.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(String.format("Unable to create JAXBContext: %s ", e.getMessage()), e);
        }
    }

    public static Manifest read(InputStream inputStream) {
        return new OasisManifest(inputStream).getManifest();
    }

    private Manifest manifest = new Manifest();

    public OasisManifest(MimeType mimeType) {
        add("/", mimeType);
    }

    public OasisManifest(InputStream inputStream) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            manifest = (Manifest) unmarshaller.unmarshal(inputStream);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to read XML as OASIS OpenDocument Manifest.", e);
        }
    }

    public void add(String path, MimeType mimeType) {
        FileEntry fileEntry = new FileEntry();
        fileEntry.setMediaType(mimeType.toString());
        fileEntry.setFullPath(path);
        manifest.getFileEntry().add(fileEntry);
    }

    public void append(OasisManifest oasisManifest) {
        for (FileEntry fileEntry : oasisManifest.getManifest().getFileEntry())
            if (!fileEntry.getFullPath().equals("/"))
                manifest.getFileEntry().add(fileEntry);
    }

    public int size() {
        return manifest.getFileEntry().size();
    }

    public Manifest getManifest() {
        return manifest;
    }

    public byte[] toBytes() {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            marshaller.marshal(manifest, byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create OASIS OpenDocument Manifest.", e);
        }
    }
}
