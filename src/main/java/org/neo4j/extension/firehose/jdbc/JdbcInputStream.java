package org.neo4j.extension.firehose.jdbc;

import au.com.bytecode.opencsv.CSVWriter;
import org.neo4j.extension.firehose.helper.JdbcHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.Properties;

public class JdbcInputStream extends InputStream {

    private final PipedReader pipedReader;
    private Throwable exceptionFromWorkerThread;

    public JdbcInputStream(String jdbcBase, Properties jdbcProperties) {
        pipedReader = new PipedReader();

        // we need to wait until a) data from jdbc is available or b) a underlying execption has been catched.
        // 'ready' holds this information
        final Boolean[] ready = {false};

        Thread workerThread = new Thread(() -> {
            try (PipedWriter pipedWriter = new PipedWriter(pipedReader)) {
                JdbcHelper.runSqlAndConsume(jdbcBase, jdbcProperties, resultSet -> {
                    try (CSVWriter csvWriter = new CSVWriter(pipedWriter)) {
                        ready[0] = true;
                        csvWriter.writeAll(resultSet, true);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
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
