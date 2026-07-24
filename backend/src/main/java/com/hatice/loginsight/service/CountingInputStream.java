package com.hatice.loginsight.service;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CountingInputStream extends FilterInputStream {

    private long bytesRead = 0;

    public CountingInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int result = super.read();
        if (result != -1) {
            bytesRead++;
        }
        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = super.read(b, off, len);
        if (result != -1) {
            bytesRead += result;
        }
        return result;
    }

    public long getBytesRead() {
        return bytesRead;
    }
}