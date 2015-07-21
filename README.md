# ASiC - Associated Signature Container

An ASiC file is simply a ZIP archive created according to some rules set forth in the specifications. 

The benefits of using containers for message transfer are:
* all files are kept together as a single collection.
* very efficient with regards to space.
* due to the compressed format, communication bandwith is utilized better
* message integrity is provided, using message digests and signatures.

This component provides an easy-to-use factory for creating ASiC-E containers according to 
[ETSI TS 102 918](http://webapp.etsi.org/workprogram/Report_WorkItem.asp?WKI_ID=42455).

This implementation uses CAdES (CMS Advanced Electronic Signature) 
for signatures rather than XAdES, both of which are allowed according to the ASiC specification.

## What does it look like?
In general the archive looks something like depicted below 

```
asic-container.asice: 
   |
   +-- mimetype
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


Consult the [AsicCadesContainerWriter](src/test/java/AsicCadesContainerWriter.java) for sample usage.


## Current status

This component is currently work-in-progress.

These features have not yet been implemented:

* Verification of ASiC container.
* Proper signing when using XAdES format for signatures.
 

## Creating an ASiC-E container manually

This is how you create an ASiC container manually:

1. Create empty directory named `asic-sample`
1. Copy the files `bii-envelope.xml`and `bii-message.xml` into `asic-sample`
1. Create the directory `META-INF`:
1. Compute the SHA-256 digest value for the files and save them:
```
openssl dgst -sha256 -binary bii-envelope |base64
openssl dgst -sha256 -binary bii-message |base64

```
1. Create the file `META-INF/asicmanifest.xml`, add an entry for each file and
paste the SHA-256 values computed in the previous step. The file should look something like this:
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
1. Create the signature, which should be placed into `signature.p7s`. The file `comodo.pem` should
be replaced with the PEM-file holding your private key for the signature, and the certificate to prove it.
```
openssl cms -sign -in META-INF/asicmanifest.xml -binary -outform der -out META-INF/signature.p7s -signer comodo.pem
```

1. Verify the signature:
```
openssl cms -verify -in META-INF/signature.p7s -inform der -content META-INF/asicmanifest.xml -noverify
```
Note! The `-noverify` option omits verifying the certificate chain of trust and should only be used to verify that the files were created properly

1. Create the ZIP-archive using your favourite tool :-)

**Disclaimer:** The procedure liste above works on a Mac or Linux machine with the various tools pre-installed. If you are running on a windows machine
you need to download and install the *openssl* and *base64* tool and adapt the procedure according to your liking.


## Verifying the contents using *openssl*

Here is how to verify the signature using the *openssl(1)* command line tool:

```
openssl cms -verify -in META-INF/signature.p7s -inform der -content META-INF/asicmanifest.xml -noverify
```

The `-noverify` option will allow self signed certificates, and should normally be omitted :-).
