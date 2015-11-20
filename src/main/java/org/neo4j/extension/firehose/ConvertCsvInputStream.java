package org.neo4j.extension.firehose;

import au.com.bytecode.opencsv.CSVWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.function.BiConsumer;

public class ConvertCsvInputStream extends InputStream {

    private final PipedReader pipedReader = new PipedReader();
    private Throwable exceptionFromWorkerThread;

    public ConvertCsvInputStream(BiConsumer<CSVWriter, Boolean[]> consumer) {
        // we need to wait until a) data from consumer is available or b) a underlying exception has been catched.
        // 'ready' holds this information
        final Boolean[] ready = {false};

        Thread workerThread = new Thread(() -> {
            try (PipedWriter pipedWriter = new PipedWriter(pipedReader)) {
                try (CSVWriter csvWriter = new CSVWriter(pipedWriter)) {
                    consumer.accept(csvWriter, ready);
                    ready[0] = true;   // safety net
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        workerThread.setUncaughtExceptionHandler((thread, throwable) -> {
            exceptionFromWorkerThread = throwable.getCause();
            ready[0] = true;
        });
        workerThread.start();

        while (!ready[0]) {
            try {
                Thread.currentThread().sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int read() throws IOException {
        if (exceptionFromWorkerThread != null) {
            if (exceptionFromWorkerThread instanceof IOException) {
                throw (IOException)exceptionFromWorkerThread;
            } else {
                throw new IOException(exceptionFromWorkerThread);
            }
        }
        return pipedReader.read();
    }

    @Override
    public void close() throws IOException {
        pipedReader.close();
    }
}
