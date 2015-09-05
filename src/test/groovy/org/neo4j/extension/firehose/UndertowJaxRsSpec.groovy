package org.neo4j.extension.firehose

import groovyx.net.http.RESTClient
import io.undertow.Undertow
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer
import org.junit.Rule
import org.reflections.Reflections
import spock.lang.Specification

import javax.ws.rs.Path
import javax.ws.rs.core.Application

class UndertowJaxRsSpec extends Specification {

    @Rule
    SampleDbRule sampleDb = new SampleDbRule()

    def "run jax rs resource with undertow"() {

        setup:
        UndertowJaxrsServer ut = new UndertowJaxrsServer()
        ut.deploy(new Application() {
            @Override
            Set<Class<?>> getClasses() {
                Reflections reflections = new Reflections("org.neo4j.extension.firehose")
                reflections.getTypesAnnotatedWith(Path)
            }
        })

        ut.start(Undertow.builder().addHttpListener(8080, "localhost"))

        when:

        def client = new RESTClient("http://localhost:8080")

        def resp = client.get(path:"/jdbc", query: [url: sampleDb.jdbc, table: "Person"])

        then:
        resp.status == 200
        resp.data.text == '"ID","NAME"\n"1","John"\n"2","Jim"\n'

        cleanup:
        ut?.stop()


    }
}
