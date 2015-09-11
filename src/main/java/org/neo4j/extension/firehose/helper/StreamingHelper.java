package org.neo4j.extension.firehose.helper;

import au.com.bytecode.opencsv.CSVWriter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.util.function.Consumer;

public class StreamingHelper {

    /**
     * stream a response based on a {@link CSVWriter}
     * @param csvWriterCallback callback
     * @return
     */
    public static Response streamCsvResponse(Consumer<CSVWriter> csvWriterCallback) {
        StreamingOutput stream = output -> {
            try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(output))) {
                csvWriterCallback.accept(csvWriter);
            }
        };
        return Response.ok(stream).build();
    }
}
