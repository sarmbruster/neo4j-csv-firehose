package org.neo4j.extension.firehose.jdbc

import org.junit.Rule
import org.neo4j.extension.firehose.SampleDbRule
import org.neo4j.extension.spock.Neo4jResource
import org.neo4j.graphdb.QueryExecutionException
import spock.lang.Specification

class LoadCsvSpec extends Specification {

    @Rule
    @Delegate
    Neo4jResource neo4j = new Neo4jResource()

    @Rule
    SampleDbRule sample = new SampleDbRule();

    def setup() {
        System.setProperty("java.protocol.handler.pkgs", "org.neo4j.extension.firehose")
    }

    def "load csv works with jdbc"() {

        when:
        "load csv with headers from 'jdbc:h2:mem:devDb?table=Person' as line create (:Person{id:line.ID, name:line.NAME})".cypher()

        then:
        notThrown(QueryExecutionException)
        "match (n:Person) return count(n) as n".cypher()[0].n == 2
    }
}
