package org.neo4j.extension.firehose

import groovy.sql.Sql
import org.junit.Rule
import org.neo4j.extension.spock.Neo4jServerResource
import org.neo4j.graphdb.QueryExecutionException
import spock.lang.Ignore
import spock.lang.Specification

import static java.net.URLEncoder.encode

class AuthenticatorSpec extends Specification {

    @Rule
    @Delegate
    Neo4jServerResource neo4j = new Neo4jServerResource(
            thirdPartyJaxRsPackages: [ "org.neo4j.extension.firehose" : "/csv"],
            config:[
                    "dbms.security.auth_enabled": "true",
                    "extension.csv.http.username": "neo4j",
                    "extension.csv.http.password": "123"
            ]
    )

    @Rule
    SampleDbRule sampleDb = new SampleDbRule()


    @Ignore("need to check why authenticator is not used")
    def "access to csv endpoint should work with authentication"() {

        setup:
//        println new URL("http://localhost:7474/csv/jdbc").text
/*
        InputStream is = new URL("http://www.heise.de").openStream();
                        BufferedReader r = new BufferedReader(new InputStreamReader(is));
                        System.out.println(r.readLine());
                        r.close();
*/

        when:
        """load csv with headers from '${baseUrl}${url}' as line create (:Person{id:line.ID, name:line.NAME})""".cypher()

        then:
        notThrown(QueryExecutionException)
        "match (n:Person) return count(n) as n".cypher()[0].n == count

        where:
        url                                           | count
        "csv/jdbc?url=jdbc:h2:mem:devDb&table=Person" | 2

    }

}
