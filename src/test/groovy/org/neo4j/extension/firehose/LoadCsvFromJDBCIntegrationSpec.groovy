package org.neo4j.extension.firehose

import groovy.sql.Sql
import org.junit.Rule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.QueryExecutionException
import spock.lang.Specification

import static java.net.URLEncoder.encode

class LoadCsvFromJDBCIntegrationSpec extends Specification {

    @Rule
    @Delegate
    Neo4jServerResource neo4j = new Neo4jServerResource(
           thirdPartyJaxRsPackages: [ "org.neo4j.extension.firehose" : "/csv"]
    )

    def "import from h2 database"() {

         def db = Sql.newInstance("jdbc:h2:mem:devDb")
        db.execute("""
create table person (
    id int auto_increment primary key,
    name varchar(20)

)""")
        db.execute("insert into person (name) values (?)", ["John"])
        assert db.updateCount == 1

        db.execute("insert into person (name) values (?)", ["Jim"])
        assert db.updateCount == 1

        when:
        """load csv with headers from '${baseUrl}${url}' as line create (:Person{id:line.ID, name:line.NAME})""".cypher()

        then:
        notThrown(QueryExecutionException)
        "match (n:Person) return count(n) as n".cypher()[0].n == count

        cleanup:
        db.execute("drop table person")

        where:
        url                                                                                 | count
        "csv/jdbc?url=jdbc:h2:mem:devDb&table=Person"                                           | 2
        "csv/jdbc?url=jdbc:h2:mem:devDb&sql=${encode("select * from Person limit 1", 'UTF-8')}" | 1

    }

}
