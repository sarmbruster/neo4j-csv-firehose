package org.neo4j.extension.firehose

import org.junit.ClassRule
import org.junit.Rule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.QueryExecutionException
import spock.lang.Shared
import spock.lang.Specification

import static java.net.URLEncoder.encode

class LoadCsvFromJDBCIntegrationSpec extends Specification {

    @Rule
    @Delegate
    Neo4jServerResource neo4j = new Neo4jServerResource(
           thirdPartyJaxRsPackages: [ "org.neo4j.extension.firehose" : "/csv"]
    )

    @ClassRule
    @Shared
    SampleDbRule sampleDb = new SampleDbRule()

    def "import from h2 database"() {

        when:
        """load csv with headers from '${baseUrl}${url}' as line create (:Person{id:line.ID, name:line.NAME})""".cypher()

        then:
        notThrown(QueryExecutionException)
        "match (n:Person) return count(n) as n".cypher()[0].n == count

        where:
        url                                                                                    | count
        "csv/jdbc?url=${sampleDb.jdbc}&table=Person"                                           | 2
        "csv/jdbc?url=${sampleDb.jdbc}&sql=${encode("select * from Person limit 1", 'UTF-8')}" | 1

    }

}
