package no.difi.asic;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

public class AsicUtils {

    AsicUtils() {
        // No action
    }

    public static void combine(OutputStream outputStream, InputStream... inputStreams) throws IOException {
        AsicOutputStream target = new AsicOutputStream(outputStream);
        int counter = 0;

        for (InputStream inputStream : inputStreams) {
            AsicInputStream source = new AsicInputStream(inputStream);

            ZipEntry zipEntry;
            while ((zipEntry = source.getNextEntry()) != null) {
                // TODO Better code to make sure manifest filenames doesn't collide.
                if (zipEntry.getName().equals("META-INF/asicmanifest.xml"))
                    zipEntry = new ZipEntry(String.format("META-INF/asicmanifest%s.xml", ++counter));

                target.putNextEntry(zipEntry);
                IOUtils.copy(source, target);
                source.closeEntry();
                target.closeEntry();
            }

            source.close();
        }

        target.close();
    }

}