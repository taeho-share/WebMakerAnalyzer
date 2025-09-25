package com.bizflow.bas.webmaker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogWriter {
    private BufferedWriter writer;
    private String filePath;

    /**
     * Creates a new LogWriter that writes to the specified file.
     *
     * @param filePath The path of the log file
     */
    public LogWriter(String filePath) throws IOException {
        this.filePath = filePath;
        this.writer = new BufferedWriter(new FileWriter(filePath));
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Writes a message to the log file.
     */
    public void writeMessage(String message) throws IOException {
        writer.write("== " + message + " ==");
        writer.newLine();
    }

    /**
     * Writes an XML element to the log file.
     */
    public void writeElement(String element) throws IOException {
        writer.write(element);
        writer.newLine();
        writer.write("----------------------------------------");
        writer.newLine();
    }

    /**
     * Closes the log file.
     */
    public void close() throws IOException {
        writer.close();
    }
}