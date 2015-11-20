package org.neo4j.extension.firehose.helper;

import org.neo4j.function.Consumer;

import java.sql.*;
import java.util.Properties;

public class JdbcHelper {

    /**
     * access a JDBC database, run a SQL statement and consume the result by a @link{Consumer}
     * uses try-with-resource to guarantee cleanup
     *
     * @param jdbcString jdbc connect string
     * @param props properties to pass in for getting the connection
     * @param resultConsumer a function that consumes the resultset
     */
    public static void runSqlAndConsume( String jdbcString, Properties props, Consumer<ResultSet> resultConsumer) {
        try (Connection connection = DriverManager.getConnection(jdbcString, props)) {
            try (Statement statement = connection.createStatement()) {

                String sql = (String) props.get("sql");
                String table = (String) props.get("table");

                if (sql == null) {
                    sql = String.format("select * from %s", table);
                }
                try (ResultSet resultSet =  statement.executeQuery(sql)) {
                    resultConsumer.accept(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
