package org.neo4j.extension.firehose;

import org.neo4j.extension.firehose.helper.JdbcHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

import static org.neo4j.extension.firehose.helper.StreamingHelper.*;

/**
 * unmanaged extension to neo4j exposing jdbc as a csv file. Cypher's LOAD CSV can use that by doing
 * LOAD CSV WITH HEADERS FROM "http://localhost:7474/csv/jdbc?url=jdbc:mysql....&user=myuser&password=mypass"
 */
@Path("/jdbc")
public class JdbcHandler {

    @GET
    public Response jdbcAsCsv(
            @Context UriInfo uriInfo,
            @QueryParam("url") String jdbcString
    ) {
        final Properties props = new Properties();
        uriInfo.getQueryParameters().forEach((key, values) -> {
            if (values.size() > 1) {
                throw new IllegalArgumentException("cannot have more than one value for query param " + key);
            }
            props.put(key, values.get(0));
        });

        return streamCsvResponse(
                csvWriter -> JdbcHelper.runSqlAndConsume(jdbcString, props, resultSet -> {
                    try {
                        csvWriter.writeAll(resultSet, true);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

}