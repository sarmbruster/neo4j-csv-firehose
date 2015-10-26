package org.neo4j.extension.firehose

import groovy.json.JsonSlurper
import org.junit.ClassRule
import org.junit.Rule
import org.neo4j.extension.firehose.helper.JdbcMetaHelper
import org.neo4j.extension.spock.Neo4jServerResource
import spock.lang.Shared
import spock.lang.Specification

class MetaDataFromJDBCSpec extends Specification {

    @ClassRule
    @Shared
    SampleDbRule sampleDb = new SampleDbRule()

    def "metadata from h2 database"() {

        when:
        def metadata = JdbcMetaHelper.metaInfo(database,new Properties())


        then:
        notThrown(Exception)
        metadata.nodes.size() == nodes

        def table = metadata.nodes[0]
        table.filename == filename
        table.labels == labels
        table.properties.size() == properties

        def prop = table.properties
        prop*.headerKey == headerKey
        prop*.primaryKey == primaryKey
        prop*.dataType == type

        where:
        database                            | nodes | filename | labels     | properties | headerKey      | neoKey        | primaryKey   | type
        sampleDb.jdbc                       | 1     | "PERSON" | ["Person"] | 2          | ["ID","NAME"]  | ["id","name"] | [true,false] | ["string","string"]

    }
}
