package org.neo4j.extension.firehose;

import com.opencsv.CSVWriter;
import org.neo4j.function.Function;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.sql.*;

import static org.neo4j.extension.firehose.StreamingHelper.*;

@Path("/jdbc")
public class JdbcHandler {

    @GET
    @Path("/{jdbcString}")
    public Response jdbcAsCsv(
            @PathParam("jdbcString") String jdbcString,
            @QueryParam("sql") String sql,
            @QueryParam("table") String table,
            @QueryParam("user") String user,
            @QueryParam("password") String password
            ) {
        try {
            Connection connection = DriverManager.getConnection(jdbcString, user, password);
            Statement statement = connection.createStatement();

            if (sql == null) {
                sql = String.format("select * from %s", table);
            }
            final ResultSet resultSet = statement.executeQuery(sql);

            return streamCsvResponse(
                    new Function<CSVWriter, Void>() {
                        @Override
                        public Void apply(CSVWriter csvWriter) throws RuntimeException {
                            try {
                                csvWriter.writeAll(resultSet, true);
                                resultSet.close();
                                return null;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }

            );

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
