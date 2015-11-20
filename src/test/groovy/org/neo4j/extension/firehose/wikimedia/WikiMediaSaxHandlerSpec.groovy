package org.neo4j.extension.firehose.wikimedia

import org.neo4j.helpers.Pair
import org.xml.sax.helpers.DefaultHandler
import spock.lang.Specification

import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

/**
 * @author Stefan Armbruster
 */
class WikiMediaSaxHandlerSpec extends Specification {

    def "test parsing"() {

        when:
        def now = System.currentTimeMillis();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
        BlockingQueue<Pair<String,String>> queue = new ArrayBlockingQueue<>(50000);

        new Thread({
            DefaultHandler handler = new WikiMediaSaxHandler(queue);
//            InputStream inputStream = new File("/mnt/virtualbox/dewiki-20150826-pages-articles-multistream.xml").newInputStream()
            InputStream inputStream = WikiMediaSaxHandlerSpec.class.getResourceAsStream("/dewiki-20150826-pages-articles-multistream_snippet.xml")
            parser.parse(inputStream, handler);
        }).start()

        def count = 0
        Pair<String,String> pair
        while (!(pair = queue.take()).equals(WikiMediaSaxHandler.POISON)) {
            if ((count > 18772) && (count<18780)) {
                println "${count}: $pair"
            }

            count++
        }

        println "took ${System.currentTimeMillis()-now}"
        then:
        true
    }

}