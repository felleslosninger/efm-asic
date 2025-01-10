package no.difi.asic.zipbomb;

import com.google.common.io.ByteStreams;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

public class MaxSizeProtectedOutputStreamTest {

    @Test(
        expectedExceptions = IllegalArgumentException.class,
        expectedExceptionsMessageRegExp = "Max size should be at least 1 KiB"
    )
    public void assureSanity() {
        MaxSizeProtectedOutputStream contentsOfStream = new MaxSizeProtectedOutputStream(1023);
    }

    @Test
    public void shouldCopyNormallySinceLessThanOneMegabyte() throws IOException {
        InputStream is = getClass().getResourceAsStream("/asic-cades-test-valid.asice");
        MaxSizeProtectedOutputStream contentsOfStream = new MaxSizeProtectedOutputStream();
        ByteStreams.copy(is, contentsOfStream);
    }

    @Test(
        expectedExceptions = RuntimeException.class,
        expectedExceptionsMessageRegExp = "Output exceeds max configured size of 1025, aborted after .* bytes"
    )
    public void shouldAbortReadingAfterExceedingLimit() throws IOException {
        InputStream is = getClass().getResourceAsStream("/asic-cades-test-valid.asice");
        MaxSizeProtectedOutputStream contentsOfStream = new MaxSizeProtectedOutputStream(1025);
        ByteStreams.copy(is, contentsOfStream);
    }

}