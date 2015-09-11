package org.neo4j.extension.firehose.jdbc

import org.junit.Rule
import org.neo4j.extension.firehose.SampleDbRule
import spock.lang.Ignore
import spock.lang.Specification

import java.sql.SQLException

class UrlSpec extends Specification {

    @Rule
    SampleDbRule sample = new SampleDbRule();

    def setup() {
        System.setProperty("java.protocol.handler.pkgs", "org.neo4j.extension.firehose")
    }

    def "a custom URLStreamFactory is used for 'jdbc' URLs"() {
        /*
        URL.setURLStreamHandlerFactory(
                new URLStreamHandlerFactory() {
                    @Override
                    URLStreamHandler createURLStreamHandler(String protocol) {
                        return null
                    }
                }
        )
*/
        when:
        assert  sample.jdbc.startsWith("jdbc:")
        def url = new URL("${sample.jdbc}?table=person")
        def lines = url.openStream().readLines()

        then:
        noExceptionThrown()
        lines == ['"ID","NAME"',
                  '"1","John"',
                  '"2","Jim"']
    }

    def "in case of a SQLException we get don't run endless"() {
        when:
        def url = new URL("${sample.jdbc}?table=person2")
        def lines = url.openStream().readLines()

        then:
        def e = thrown(IOException)
        e.cause instanceof SQLException
    }

    def "access mysql db"() {
        when:
        def url = new URL("jdbc:mysql://localhost/neg?user=neg&password=neg&table=urkunde")

        then:
        noExceptionThrown()
        url.openStream().readLines().size() == 2142 +1 // header line
    }
}
