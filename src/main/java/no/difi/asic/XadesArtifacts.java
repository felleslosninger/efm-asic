package no.difi.asic;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XadesArtifacts {
    private final Document document;
    private final Element signableProperties;
    private final String signablePropertiesReferenceUri;

    public XadesArtifacts(Document document, Element signableProperties, String signablePropertiesReferenceUri) {
        this.document = document;
        this.signableProperties = signableProperties;
        this.signablePropertiesReferenceUri = signablePropertiesReferenceUri;
    }

    public Document getDocument() {
        return document;
    }

    public Element getSignableProperties() {
        return signableProperties;
    }

    public String getSignablePropertiesReferenceUri() {
        return signablePropertiesReferenceUri;
    }
}