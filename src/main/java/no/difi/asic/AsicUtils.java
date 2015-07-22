package no.difi.asic;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;

public class AsicUtils {

    public static void combine(OutputStream outputStream, InputStream... inputStreams) throws IOException {
        AsicOutputStream target = new AsicOutputStream(outputStream);

        for (InputStream inputStream : inputStreams) {
            AsicInputStream source = new AsicInputStream(inputStream);

            ZipEntry zipEntry;
            while ((zipEntry = source.getNextEntry()) != null) {
                // TODO Interfere to make sure manifest filenames doesn't collide.

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
