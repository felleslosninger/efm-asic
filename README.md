> [!WARNING]
> Please note that this project is in maintenance mode.
> 
> It is currently being maintained for the purpose of eFormidlings use of the advanced signature container (ASiC) format.
>
> Bugs will be fixed and dependencies kept up to date, but no new features will be added.
>
> eFormidling is looking into alternatives and might archive the project in the future.
>
> Alternatives to this project include:
>
> - KS FIKS is maintaining a fork of this libarary at https://github.com/ks-no/asic (also for .NET at https://github.com/ks-no/fiks-asice-dotnet)
> - The Digital Signature Service (part of the Digital Europe eSignature building block) is maintaining a set of libraries for building and validating ASiC (and more) at https://github.com/esig/dss

# Associated Signature Container (ASiC)

An ASiC file is simply a ZIP archive created according to some rules set forth in the specifications. 

The benefits of using containers for message transfer are:
* all files are kept together as a single collection.
* very efficient with regards to space.
* due to the compressed format, communication bandwith is utilized better
* message integrity is provided, using message digests and signatures.
* confidentiality is provied by encryption using AES-256 in GCM mode

This component provides an easy-to-use factory for creating ASiC-E containers.

Conformance is claimed according to 7.2.1 (TBA) and 7.2.2 in
[ETSI TS 102 918 V1.3.1](http://webapp.etsi.org/workprogram/Report_WorkItem.asp?WKI_ID=42455).


## Maven
Latest `1.x.y` series is for Java 21+, while the older series based on Java 8 ended with version `0.12.0` release on `19 Oct 2023`.

<table>
<tr>
<th>Java 21 (in maintenance mode)</th>
<th>Java 8 (no longer maintained)</th>
</tr>
<tr>
<td>

```xml
<dependency>
    <groupId>no.difi.commons</groupId>
    <artifactId>commons-asic</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

</td>
<td>

```xml
<dependency>
    <groupId>no.difi.commons</groupId>
    <artifactId>commons-asic</artifactId>
    <version>0.12.0</version>
</dependency>
```

</td>
</tr>
</table>

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

Consult the [AsicCadesContainerWriterTest](src/test/java/no/difi/asic/AsicWriterTest.java) for sample usage.
Here is a rough sketch on how to do it:
```java
// Creates an ASiC archive after which every entry is read back from the archive.

// Name of the file to hold the the ASiC archive
File archiveOutputFile = new File(System.getProperty("java.io.tmpdir"), "asic-sample-default.zip");

// Creates an AsicWriterFactory with default signature method
AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();

// Creates the actual container with all the data objects (files) and signs it.
AsicWriter asicWriter = asicWriterFactory.newContainer(archiveOutputFile)
        // Adds an ordinary file, using the file name as the entry name
        .add(biiEnvelopeFile)
                // Adds another file, explicitly naming the entry and specifying the MIME type
        .add(biiMessageFile, BII_MESSAGE_XML, MimeType.forString("application/xml"))
                // Signing the contents of the archive, closes it for further changes.
        .sign(keystoreFile, TestUtil.keyStorePassword(), TestUtil.privateKeyPassword());

// Opens the generated archive and reads each entry
AsicReader asicReader = AsicReaderFactory.newFactory().open(archiveOutputFile);

String entryName;

// Iterates over each entry and writes the contents into a file having same name as the entry
while ((entryName = asicReader.getNextFile()) != null) {
    log.debug("Read entry " + entryName);
    
    // Creates file with same name as entry
    File file = new File(entryName);
    // Ensures we don't overwrite anything
    if (file.exists()) {
        throw new IllegalStateException("File already exists");
    }
    asicReader.writeFile(file);
    
    // Removes file immediately, since this is just a test 
    file.delete();  
}
asicReader.close(); 
```


## Security

This library validate signatures, but does not validate the certificate. It's up to the implementer using the library
to choose if and how to validate certificates. Certificate(s) used for validation is exposed by the library.


## Creating an ASiC-E container manually

This is how you create an ASiC container manually:

1. Create empty directory named `asic-sample`
1. Copy the files `bii-envelope.xml`and `bii-trns081.xml` into `asic-sample`
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
<ASiCManifest xmlns="http://uri.etsi.org/02918/v1.2.1#" xmlns:ns2="http://www.w3.org/2000/09/xmldsig#">
    <SigReference URI="META-INF/signature.p7s" MimeType="application/x-pkcs7-signature"/>
    <DataObjectReference URI="bii-trns081.xml" MimeType="application/xml">
        <ns2:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
        <ns2:DigestValue>morANIlh3TGxMUsJWKfICly7YXoduG7LCohAKc2Sip8=</ns2:DigestValue>
    </DataObjectReference>
    <DataObjectReference URI="bii-envelope.xml" MimeType="application/xml">
        <ns2:DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256"/>
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


## Programmers notes

You might encounter memory problems when using Java 1.7. This is due to the memory consumption of JAXB.

Try this before you run maven, you might need to increase this even further (your mileage may vary):
```
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
```
or on Windows:
```
set MAVEN_OPTS=-Xmx1024m -XX:MaxPermSize=512m
```
