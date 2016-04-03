package no.difi.asic;

import java.io.IOException;
import java.io.InputStream;

class InputStreamWrapper extends InputStream {

    private InputStream source;

    public InputStreamWrapper(InputStream source) {
        this.source = source;
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }
}
