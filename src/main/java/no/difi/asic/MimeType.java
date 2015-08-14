package no.difi.asic;

public class MimeType {

    public static final MimeType XML = MimeType.forString("application/xml");

    public static MimeType forString(String mimeType) {
        return new MimeType(mimeType);
    }

    private String mimeType;

    private MimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    @Override
    public String toString() {
        return mimeType;
    }
}
