package org.neo4j.extension.firehose.jdbc;

import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class JdbcUrlConnection extends URLConnection {
    private final String jdbcBase;
    private final Properties jdbcProperties = new Properties();

    @Override
    public void connect() throws IOException {
        throw new UnsupportedOperationException();
    }

    protected JdbcUrlConnection(URL url) {
        super(url);

        String urlAsString = url.toString();
        int position = urlAsString.indexOf("?");
        if (position == -1) {
            jdbcBase = urlAsString;
        } else {
            jdbcBase = urlAsString.substring(0, position);
            String rest = urlAsString.substring(position+1);
            MultiMap<String> map = new MultiMap<>();
            UrlEncoded.decodeTo(rest, map, (String) null, 10);

            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                if (entry.getValue().size()>1) {
                    throw new IllegalArgumentException("parameter " + entry.getKey() + " occurs more than once");
                }
                jdbcProperties.put(entry.getKey(), entry.getValue().get(0));
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new JdbcInputStream(jdbcBase, jdbcProperties);
    }

}
