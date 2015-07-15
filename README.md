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

