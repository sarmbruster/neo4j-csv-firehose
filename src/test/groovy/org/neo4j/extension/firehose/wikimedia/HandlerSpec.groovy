package org.neo4j.extension.firehose.wikimedia

import org.junit.Rule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.graphdb.QueryExecutionException
import org.neo4j.helpers.collection.IteratorUtil
import spock.lang.Specification

class HandlerSpec extends Specification {

    @Rule
    @Delegate
    Neo4jResource neo4j = new Neo4jResource()

    def setup() {
        System.setProperty("java.protocol.handler.pkgs", "org.neo4j.extension.firehose")
    }

    def "load csv works with wikimedia xml"() {
        when:
        def url = HandlerSpec.class.getResource("/dewiki-20150826-pages-articles-multistream_snippet.xml")

        def result = IteratorUtil.asCollection( // need to materialize the iterator since we access multiple times
                "load csv with headers from 'wikimedia:file://${url.file}' as line return line.title as title, line.reference as reference".cypher()
        )

        then:
        notThrown(QueryExecutionException)
        result.every {
            (it.title != null) && (it.reference!=null)
        }
        result.size() == 152
    }
}
