package org.neo4j.extension.firehose.wikimedia;

import org.neo4j.extension.firehose.ConvertCsvInputStream;
import org.neo4j.helpers.Pair;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Stefan Armbruster
 */
public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {

        return new URLConnection(u) {
            @Override
            public void connect() throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream getInputStream() throws IOException {

                BlockingQueue<Pair<String,String>> queue = new ArrayBlockingQueue<>(50000);

                // feed queue from different thread
                new Thread(() -> {
                    SAXParser parser = null;
                    try {
                        parser = SAXParserFactory.newInstance().newSAXParser();
                        URL xmlUrl = new URL(getURL().getFile());
                        parser.parse(xmlUrl.openStream(), new WikiMediaSaxHandler(queue));
                    } catch (ParserConfigurationException e) {
                        throw new RuntimeException(e);
                    } catch (SAXException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }).start();

                return new ConvertCsvInputStream((csvWriter, isReady) -> {
                    try {
                        csvWriter.writeNext(new String[]{"title","reference"});
                        isReady[0] = true;
                        Pair<String, String> pair;
                        while (!(pair = queue.take()).equals(WikiMediaSaxHandler.POISON) ) {
                            csvWriter.writeNext(new String[] { pair.first(), pair.other()});
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        };
    }
}
