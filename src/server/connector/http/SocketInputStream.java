package server.connector.http;

import org.apache.catalina.util.StringManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class SocketInputStream extends InputStream {

    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte SP = (byte) ' ';
    private static final byte HT = (byte) '\t';
    private static final byte COLON = (byte) ':';
    private static final int LC_OFFSET = 'A' - 'a';
    protected byte buf[];
    protected int count;
    protected int pos;
    protected InputStream is;
    public SocketInputStream(InputStream is, int bufferSize) {

        this.is = is;
        buf = new byte[bufferSize];

    }
    protected static StringManager sm =
            StringManager.getManager(Constants.Package);
    public void readRequestLine(HttpRequestLine requestLine)
            throws IOException {

        if (requestLine.methodEnd != 0)
            requestLine.recycle();

        int chr = 0;
        do {
            try {
                chr = read();
            } catch (IOException e) {
                chr = -1;
            }
        } while ((chr == CR) || (chr == LF));
        if (chr == -1)
            throw new EOFException
                    (sm.getString("requestStream.readline.error"));
        pos--;


        int maxRead = requestLine.method.length;
        int readStart = pos;
        int readCount = 0;

        boolean space = false;

        while (!space) {
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_METHOD_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.method, 0, newBuffer, 0,
                            maxRead);
                    requestLine.method = newBuffer;
                    maxRead = requestLine.method.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.methodEnd = readCount - 1;

        maxRead = requestLine.uri.length;
        readStart = pos;
        readCount = 0;

        space = false;

        boolean eol = false;

        while (!space) {
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.uri, 0, newBuffer, 0,
                            maxRead);
                    requestLine.uri = newBuffer;
                    maxRead = requestLine.uri.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            } else if ((buf[pos] == CR) || (buf[pos] == LF)) {
                // HTTP/0.9 style request
                eol = true;
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }

        requestLine.uriEnd = readCount - 1;


        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;

        while (!eol) {
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOCOL_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(requestLine.protocol, 0, newBuffer, 0,
                            maxRead);
                    requestLine.protocol = newBuffer;
                    maxRead = requestLine.protocol.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == CR) {
                // Skip CR.
            } else if (buf[pos] == LF) {
                eol = true;
            } else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }

        requestLine.protocolEnd = readCount;
    }
    public void readHeader(HttpHeader header)
            throws IOException {

        if (header.nameEnd != 0)
            header.recycle();

        int chr = read();
        if ((chr == CR) || (chr == LF)) { // Skipping CR
            if (chr == CR)
                read();
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos--;
        }

        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;

        boolean colon = false;

        while (!colon) {
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= HttpHeader.MAX_NAME_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(header.name, 0, newBuffer, 0, maxRead);
                    header.name = newBuffer;
                    maxRead = header.name.length;
                } else {
                    throw new IOException
                            (sm.getString("requestStream.readline.toolong"));
                }
            }
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException
                            (sm.getString("requestStream.readline.error"));
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            if ((val >= 'A') && (val <= 'Z')) {
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount++;
            pos++;
        }

        header.nameEnd = readCount - 1;

        // Reading the header value (which can be spanned over multiple lines)

        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;

        int crPos = -2;

        boolean eol = false;
        boolean validLine = true;

        while (validLine) {

            boolean space = true;

            while (space) {
                if (pos >= count) {
                    int val = read();
                    if (val == -1)
                        throw new IOException
                                (sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }

            while (!eol) {
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0,
                                maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                                (sm.getString("requestStream.readline.toolong"));
                    }
                }
                if (pos >= count) {
                    int val = read();
                    if (val == -1)
                        throw new IOException
                                (sm.getString("requestStream.readline.error"));
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == CR) {
                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }

            int nextChr = read();

            if ((nextChr != SP) && (nextChr != HT)) {
                pos--;
                validLine = false;
            } else {
                eol = false;
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(header.value, 0, newBuffer, 0,
                                maxRead);
                        header.value = newBuffer;
                        maxRead = header.value.length;
                    } else {
                        throw new IOException
                                (sm.getString("requestStream.readline.toolong"));
                    }
                }
                header.value[readCount] = ' ';
                readCount++;
            }

        }

        header.valueEnd = readCount;
    }

    public int read()
            throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return buf[pos++] & 0xff;
    }

    public int available()
            throws IOException {
        return (count - pos) + is.available();
    }


    public void close()
            throws IOException {
        if (is == null)
            return;
        is.close();
        is = null;
        buf = null;
    }
    protected void fill()
            throws IOException {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0) {
            count = nRead;
        }
    }
}
