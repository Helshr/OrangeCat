package server.connector;

import server.connector.http.HttpResponse;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ResponseStream extends ServletOutputStream {

    protected boolean closed = false;

    protected int count = 0;

    protected int length = -1;

    protected boolean commit = false;

    protected HttpResponse response = null;

    protected OutputStream stream = null;

    public ResponseStream(HttpResponse response) {
        super();
        closed = false;
        commit = false;
        count = 0;
        this.response = response;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    
    public void write(byte[] b) throws IOException {
        super.write(b);
    }

    
    public void write(byte[] b, int off, int len) throws IOException {
        if (closed) throw new IOException("responseStream.write.closed");

        int actual = len;

        if ((length > 0) && ((count + len) >= length)) {
            actual = length - count;
        }
        response.write(b, off, actual);
        count += actual;
        if (actual < len) throw new IOException("responseStream.write.closed");
    }

    
    public void flush() throws IOException {
        if (closed) throw new IOException("responseStream.flush.closed");
        if (commit) {
            response.flushBuffer();
        }
    }

    
    public void close() throws IOException {
        if (closed) throw new IOException("responseStream.close.closed");
        response.flushBuffer();
        closed = true;
    }

    
    public void write(int b) throws IOException {
        if (closed) throw new IOException("responseStream.write.closed");
        if ((length > 0) && (count >= length)) throw new IOException("responseStream.write.count");

        response.write(b);
        count++;
    }
}
