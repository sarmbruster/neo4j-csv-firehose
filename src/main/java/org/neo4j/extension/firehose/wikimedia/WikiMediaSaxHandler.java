package org.neo4j.extension.firehose.wikimedia;

import org.neo4j.helpers.Pair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiMediaSaxHandler extends DefaultHandler {

    public static final Pair<String,String> POISON = Pair.of(null, null);
    private final BlockingQueue<Pair<String,String>> queue;
    private boolean inPage;
    private boolean inTitle;
    private boolean inText;
    private StringBuilder titleStringBuilder;
    private StringBuilder textStringBuilder;
    private final Pattern wikiLinkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]", Pattern.DOTALL);
    private String title;

    public WikiMediaSaxHandler(BlockingQueue<Pair<String, String>> queue) {
        this.queue = queue;
    }

    @Override
    public void endDocument() throws SAXException {
        try {
            queue.put(POISON);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("page".equals(qName)) {
            inPage = true;
        } else if ("title".equals(qName)) {
            inTitle = true;
            titleStringBuilder = new StringBuilder("\"");
        } else if ("text".equals(qName)) {
            inText = true;
            textStringBuilder = new StringBuilder();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if ("page".equals(qName)) {
            inPage = false;
        } else if ("title".equals(qName)) {
            inTitle = false;
            titleStringBuilder.append("\"");
            title = titleStringBuilder.toString();
        } else if ("text".equals(qName)) {
            inText = false;
            String text = textStringBuilder.toString();
            Matcher m = wikiLinkPattern.matcher(text);
            while (m.find()) {
                StringBuilder targetTitleBuilder = new StringBuilder("\"").append(m.group(1)).append("\"");
                try {
                    queue.put(Pair.of(title, targetTitleBuilder.toString()));
                } catch (InterruptedException e) {
                    throw new RuntimeException();
                }
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inTitle) {
            titleStringBuilder.append(ch, start, length);
        } else if (inText) {
            textStringBuilder.append(ch, start, length);
        }
    }
}
