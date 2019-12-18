package com.knifeds.kdsclient.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public class Tools {
    public static boolean isSuccess(ConsoleOutput result) {
        if (result.error.equals("")) {
            return true;
        }
        if (result.contents.size() > 0 && isSimilar("Success", result.contents.get(0))) {
            return true;
        }
        return false;
    }

    public static boolean isSimilar(String one, String another) {
        if (one == null || another == null) {
            return false;
        }
        int length = one.length();
        if (length > another.length()) {
            return false;
        }
        if (one.equalsIgnoreCase(another.substring(0, length))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * close reader, catch and ignore all exceptions.
     *
     * @param reader
     *            the reader object, may be null
     */
    public static void close(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * close writer, catch and ignore all exceptions.
     *
     * @param writer
     *            the writer object, may be null
     */
    public static void close(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * close input stream, catch and ignore all exceptions.
     *
     * @param stream
     *            the input stream object, may be null
     */
    public static void close(InputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * close output stream, catch and ignore all exceptions.
     *
     * @param stream
     *            the output stream object, may be null
     */
    public static void close(OutputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (Exception e) {
            }
        }
    }
}
