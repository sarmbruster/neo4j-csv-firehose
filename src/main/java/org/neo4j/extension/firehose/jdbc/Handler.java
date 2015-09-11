package org.neo4j.extension.firehose.jdbc;

import java.io.IOException;
import java.net.*;

/**
 * A URLStreamHandler implementation to be used for "jdbc:" like URLs
 * this package needs to be registered with the JVM:
 * -Djava.protocol.handler.pkgs=org.neo4j.extension.firehose
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new JdbcUrlConnection(u);
    }
}
