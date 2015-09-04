package org.neo4j.extension.firehose;

import com.opencsv.CSVWriter;
import org.neo4j.function.Function;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * @author Stefan Armbruster
 */
public class StreamingHelper {

    public static Response streamCsvResponse(final Function<CSVWriter,Void> csvWriterCallback) {

        StreamingOutput stream = new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(output));
                csvWriterCallback.apply(csvWriter);
                csvWriter.close();
            }
        };
        return Response.ok(stream).build();

    }

}
