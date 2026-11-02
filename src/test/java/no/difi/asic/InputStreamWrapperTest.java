package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class InputStreamWrapperTest {

    @Test
    public void bulkReadIsDelegated() throws IOException {
        byte[] content = new byte[100_000];
        for (int i = 0; i < content.length; i++)
            content[i] = (byte) i;

        // Reading one byte at a time from the source made large entries very slow to read.
        InputStream source = new FilterInputStream(new ByteArrayInputStream(content)) {
            @Override
            public int read() {
                throw new AssertionError("Single byte read used for bulk read.");
            }
        };

        Assert.assertEquals(ByteStreams.toByteArray(new InputStreamWrapper(source)), content);
    }

    @Test
    public void closeDoesNotCloseSource() throws IOException {
        boolean[] closed = {false};
        InputStream source = new FilterInputStream(new ByteArrayInputStream(new byte[1])) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };

        new InputStreamWrapper(source).close();
        Assert.assertFalse(closed[0]);
    }
}
