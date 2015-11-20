package org.neo4j.extension.firehose.jdbc

import groovy.json.JsonSlurper
import org.junit.ClassRule
import org.junit.Rule
import org.neo4j.extension.firehose.SampleDbRule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.QueryExecutionException
import spock.lang.Shared
import spock.lang.Specification

import static java.net.URLEncoder.encode

class MetaDataFromJDBCIntegrationSpec extends Specification {

    @Rule
    @Delegate
    Neo4jServerResource neo4j = new Neo4jServerResource(
           thirdPartyJaxRsPackages: [ "org.neo4j.extension.firehose" : "/csv"]
    )

    @ClassRule
    @Shared
    SampleDbRule sampleDb = new SampleDbRule()

    def "metadata from h2 database"() {

        when:
        def metadata = new JsonSlurper().parse(new URL(new URL(baseUrl),url))

        then:
        notThrown(Exception)
        metadata.nodes.size() == nodes

        def table = metadata.nodes[0]
        table.filename == filename
        table.labels == labels
        table.properties.size() == properties

        def prop = table.properties[0]
        prop.headerKey == headerKey
        prop.primaryKey == primaryKey
        prop.dataType == type

        where:
        url                                                                                    | nodes | filename | labels     | properties | headerKey | neoKey | primaryKey | type
        "csv/jdbc/meta?url=${sampleDb.jdbc}&database=${sampleDb.schema}"                       | 1     | "PERSON" | ["Person"] | 2          | "ID"      | "id"   | true       | "string"

    }
}
