# asic

Provides a simple builder for an ASiC-E container.

```
   +-- manifest
   |
   +-- bii-envelope.xml
   |
   +-- bii-document.xml
   |
   +-- META-INF/
          |
          + asicmanifest.xml
          |
          + signature.p7s
   
```


See AsicBuilderTest.java for sample usage.

Here is how to verify the signature using the *openssl(1)* command line tool:

```
openssl cms -verify -in META-INF/signature.p7s -inform der -content META-INF/asicmanifest.xml -noverify
```

The `-noverify` option will allow self signed certificates, and should normally be omitted :-).
 
## Creating an ASiC-E container manually

This is how you create an ASiC container manually:

1. Create empty directory named `asic-sample`
1. Copy the files `bii-envelop.xml`and `bii-message.xml` into `asic-sample`
1. Create the directory `META-INF`:
  `mkdir META-INF`
1. Create the file `asicmanifest.xml`, which should look like this:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ASiCManifest xmlns="http://uri.etsi.org/2918/v1.1.1#" xmlns:ns2="http://www.w3.org/2000/09/xmldsig#">
    <DataObjectReference URI="bii-message.xml" MimeType="application/xml">
        <ns2:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha256"/>
        <ns2:DigestValue>morANIlh3TGxMUsJWKfICly7YXoduG7LCohAKc2Sip8=</ns2:DigestValue>
    </DataObjectReference>
    <DataObjectReference URI="bii-envelope.xml" MimeType="application/xml">
        <ns2:DigestMethod Algorithm="http://www.w3.org/2000/09/xmldsig#sha256"/>
        <ns2:DigestValue>IZ9yiwKHsTWMcyFebi7csqOOIHohy2gPd02VSfbyUCI=</ns2:DigestValue>
    </DataObjectReference>
</ASiCManifest>
```
1. Create the signature, which should be placed into `signature.p7s`
```

```

