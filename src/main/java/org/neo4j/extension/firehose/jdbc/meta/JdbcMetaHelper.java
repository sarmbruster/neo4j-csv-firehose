package org.neo4j.extension.firehose.jdbc.meta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class JdbcMetaHelper {

    /**
     * access a JDBC database, extract the metainformation and return it as JdbcMetaData
     *
     * @param jdbcString jdbc connect string
     * @param props properties to pass in for getting the connection
     */
    public static JdbcMetaData metaInfo(String jdbcString, Properties props)  {
        JdbcMetaData jdbcMetaData = new JdbcMetaData();
        try (Connection connection = DriverManager.getConnection(jdbcString, props)) {
            Rules rules = new Rules(props.getProperty("skip","").split(","));
            TableInfo[] tables = MetaDataReader.extractTables(connection, props.getProperty("database"), rules);
            for (TableInfo table : tables) {
                jdbcMetaData.add(table,rules);
            }
            return jdbcMetaData;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
