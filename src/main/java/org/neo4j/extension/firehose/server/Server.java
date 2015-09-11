package org.neo4j.extension.firehose.server;

import io.undertow.Undertow;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.reflections.Reflections;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * start a undertow server
 */
public class Server {

    public static void main(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption("p", "port", true, "port number");
        options.addOption("n", "hostname", true, "hostname, e.g. localhost");

        CommandLine cmd = new DefaultParser().parse(options, args);

        int port = Integer.parseInt(cmd.getOptionValue("p", "8080"));
        String hostname = cmd.getOptionValue("n", "localhost");

        UndertowJaxrsServer ut = new UndertowJaxrsServer();
        ut.deploy(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                Reflections reflections = new Reflections("org.neo4j.extension.firehose");
                return reflections.getTypesAnnotatedWith(Path.class);
            }
        });

        ut.start(Undertow.builder().addHttpListener(port, hostname));
        System.out.println(String.format("Started server at http://%s:%d/  Hit ^C to stop", hostname, port));
    }
}
