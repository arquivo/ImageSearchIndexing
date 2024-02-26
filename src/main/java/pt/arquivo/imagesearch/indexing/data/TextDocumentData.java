package pt.arquivo.imagesearch.indexing.data;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.Writable;

import pt.arquivo.imagesearch.indexing.processors.ImageInformationExtractor;
import pt.arquivo.imagesearch.indexing.utils.WARCInformationParser;

public class TextDocumentData implements Comparable<LocalDateTime>, Writable, Serializable {

    public enum INLINK_TYPES {
        INTERNAL,
        EXTERNAL,
        ALL
    }

    public static final int MAX_INLINKS = 1000;
    public static final int MAX_OUTLINKS = 1000;


    /**
     * Collection where to which this  matches
     */
    private ArrayList<String> collection;

    /**
     * (W)ARC name
     */
    private String warc;

    /**
     * (W)ARC offset in bytes
     */
    private long warcOffset;

    /**
     * Mime type detected
     */
    private String mimeTypeDetected;

    /**
     * Mime type reported
     */
    private String mimeTypeReported;

    /**
     * title
     */
    private ArrayList<String> title;

    /**
     * metadata
     */
    private ArrayList<String> metadata;

    /**
     *  url split into word tokens
     */
    private ArrayList<String> urlTokens;

    /**
     *  capture timestamp
     */
    private LocalDateTime timestamp;


    /**
     * Oldest capture timestamp in the yyyyMMddHHmmss format
     */
    private String timestampString;

    /**
     * Document URL
     */
    private ArrayList<String> url;

    /**
     * Document SURT
     */
    private ArrayList<String> surt;

    /**
     * Host of the matching 
     */
    private ArrayList<String> host;

    /**
     *  content
     */
    private ArrayList<String> content;

    /** 
     * Outlinks
     */
    private Map<Outlink,Outlink> outlinks;

    /** 
     * Inlinks
     */
    private Map<Outlink,Outlink> inlinks;

    /**
     * Digest of the document
     */
    private String digestContainer;

    /**
     * Digest of the content
     */
    private String digestContent;

    public TextDocumentData(TextDocumentData other){
        this.collection = new ArrayList<>(other.collection);
        this.warc = other.warc;
        this.warcOffset = other.warcOffset;
        this.mimeTypeDetected = other.mimeTypeDetected;
        this.mimeTypeReported = other.mimeTypeReported;
        this.title = new ArrayList<>(other.title);
        this.urlTokens = new ArrayList<>(other.urlTokens);
        this.timestamp = other.timestamp;
        this.timestampString = other.timestampString;
        this.url = new ArrayList<>(other.url);
        this.surt = new ArrayList<>(other.surt);
        this.host = new ArrayList<>(other.host);
        this.content = new ArrayList<>(other.content);
        this.outlinks = new HashMap<>(other.outlinks);
        this.inlinks = new HashMap<>(other.inlinks);
        this.digestContainer = other.digestContainer;
        this.digestContent = other.digestContent;
        this.metadata = new ArrayList<>(other.metadata);
    }

    public TextDocumentData() {
        this.outlinks = new HashMap<>();
        this.inlinks = new HashMap<>();
        this.title = new ArrayList<>();
        this.collection = new ArrayList<>();
        this.content = new ArrayList<>();
        this.urlTokens = new ArrayList<>();
        this.surt = new ArrayList<>();
        this.host = new ArrayList<>();
        this.url = new ArrayList<>();
        this.metadata = new ArrayList<>();
    }

    @Override
    public int compareTo(LocalDateTime timestamp) {
        return this.timestamp.compareTo(timestamp);
    }

    @Override
    public String toString() {
        return String.format("\"%s\": \"%s\", %s", title, url, timestamp.toString());
    }

    public String getId() {
        return digestContent;
        //return timestampString + "/" + ImageSearchIndexingUtil.md5ofString(url);
    }

    // generate getters and setters
    public ArrayList<String> getCollection() {
        return collection;
    }

    public String getWarc() {
        return warc;
    }

    public long getWarcOffset() {
        return warcOffset;
    }

    public String getMimeTypeDetected() {
        return mimeTypeDetected;
    }

    public String getMimeTypeReported() {
        return mimeTypeReported;
    }

    public ArrayList<String> getTitle() {
        return title;
    }

    public ArrayList<String> getURLTokens() {
        return urlTokens;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getTimestampFormatted() {
        return String.format("%04d-%02d-%02dT%02d:%02d:%02dZ",
                timestamp.getYear(),
                timestamp.getMonthValue(),
                timestamp.getDayOfMonth(),
                timestamp.getHour(),
                timestamp.getMinute(),
                timestamp.getSecond());
    }

    public String getTimestampString() {
        return timestampString;
    }

    public ArrayList<String> getURL() {
        return url;
    }

    public ArrayList<String> getSurt() {
        return surt;
    }

    public ArrayList<String> getHost() {
        return host;
    }

    public ArrayList<String> getContent() {
        return content;
    }

    public Map<Outlink,Outlink> getOutlinks() {
        return outlinks;
    }

    public Map<Outlink,Outlink> getInlinks() {
        return inlinks;
    }

    public Map<Outlink,Outlink> getInlinks(INLINK_TYPES type) {
        switch (type) {
            case INTERNAL:
                return getInlinksInternal();
            case EXTERNAL:
                return getInlinksExternal();
            default:
                return inlinks;
        }
    }

    // These will match the inlinks to the surt of the document
    // Internal inlinks are those that match the subdomain of the document
    // s.g. links from arquivo.pt/wayback and arquivo.pt/text_search are considered the same internal
    // links to arquivo.pt from premio.arquivo.pt is considered external
    public Map<Outlink,Outlink> getInlinksInternal() {       
        Map<Outlink,Outlink> inlinksInternal = new HashMap<Outlink,Outlink>();
        for (Outlink inlink : inlinks.keySet()) {
            String domain = inlink.getSource().split("\\)")[0];
            // broader domain matching
            for (String surt : surt){
                String internalDomain = surt.split("\\)")[0];
                if (internalDomain.equals(domain)) {
                    inlinksInternal.put(inlink, inlink);
                    break;
                }
            }
        }
        return inlinksInternal;
    }

    public Map<Outlink,Outlink> getInlinksExternal() {
        Map<Outlink,Outlink> inlinksInternal = getInlinksInternal();
        Map<Outlink,Outlink> external = new HashMap<Outlink,Outlink>(inlinks);
        external.keySet().removeAll(inlinksInternal.keySet());
        return external;
    }

    public ArrayList<String> getInlinkAnchors() {
        return getInlinkAnchors(INLINK_TYPES.ALL);
    }

    public ArrayList<String> getInlinkAnchors(INLINK_TYPES type) {
        ArrayList<String> inlinkAnchors = new ArrayList<>();
        for (Outlink inlink : getInlinks(type).keySet()) {
            if (inlink.getAnchor() != null && !inlink.getAnchor().trim().isEmpty())
                inlinkAnchors.add(inlink.getAnchor());
        }
        return inlinkAnchors;
    }

    public ArrayList<String> getInlinkSurts() {
        return getInlinkSurts(INLINK_TYPES.ALL);
    }

    public ArrayList<String> getInlinkSurts(INLINK_TYPES type) {
        Set<String> inlinkSurt = new HashSet<>();
        for (Outlink inlink : getInlinks(type).keySet()) {
            if (inlink.getSurt() != null && !inlink.getSurt().trim().isEmpty())
            inlinkSurt.add(inlink.getSurt());
        }
        return new ArrayList<>(inlinkSurt);
    }

    public void addCollection(String collection) {
        if (collection.isEmpty() || this.collection.contains(collection))
            return;
        this.collection.add(collection);
    }

    public void setWarc(String warc) {
        this.warc = warc;
    }

    public void setWarcOffset(long warcOffset) {
        this.warcOffset = warcOffset;
    }

    public void setMimeTypeDetected(String mimeTypeDetected) {
        this.mimeTypeDetected = mimeTypeDetected;
    }

    public void setMimeTypeReported(String mimeTypeReported) {
        this.mimeTypeReported = mimeTypeReported;
    }

    public void addTitle(String title) {
        if (title.isEmpty() || this.title.contains(title))
            return;
        this.title.add(title);
    }

    public void addContent(String content) {
        if (content.isEmpty() || this.content.contains(content))
            return;
        this.content.add(content);
    }

    public void addURLTokens(String urlTokens) {
        if (urlTokens.isEmpty() || this.urlTokens.contains(urlTokens))
            return;
        this.urlTokens.add(urlTokens);
    }

    public void setTimestamp(String TimestampString) {
        this.timestampString = TimestampString;
        // timestampString format is YYYYMMDDHHmmss
        if (TimestampString.length() < 14) {
            TimestampString = TimestampString.concat("00000000000000").substring(0, 14);
        }
        this.timestamp = LocalDateTime.of(
                Integer.parseInt(TimestampString.substring(0, 4)),
                Integer.parseInt(TimestampString.substring(4, 6)),
                Integer.parseInt(TimestampString.substring(6, 8)),
                Integer.parseInt(TimestampString.substring(8, 10)),
                Integer.parseInt(TimestampString.substring(10, 12)),
                Integer.parseInt(TimestampString.substring(12, 14))
        );
    }
    
    public void setDigestContainer(String digestContainer) {
        this.digestContainer = digestContainer;
    }

    public void setDigestContent(String digestContent) {
        this.digestContent = digestContent;
    }

    public String getDigestContainer() {
        return digestContainer;
    }

    public String getDigestContent() {
        return digestContent;
    }

    public void addMetadata(String metadata) {
        if (metadata.isEmpty() || this.metadata.contains(metadata))
            return;
        this.metadata.add(metadata);
    }

    public ArrayList<String> getMetadata() {
        return metadata;
    }

    public void addURL(String url) {
        if (this.url.contains(url))
            return;
        this.url.add(url);
        if (!url.startsWith("http"))
            url = "http://" + url;
        URL uri;
        try {
            uri = new URL(url);
            this.host.add(uri.getHost());
        } catch (MalformedURLException e) {
            // e.printStackTrace();
            url = url.replaceAll("http://", "").replaceAll("https://", "");
            this.host.add(url.split("/")[0]);
        }
        this.urlTokens.add(ImageInformationExtractor.getURLSrcTokens(url));
        this.surt.add(WARCInformationParser.toSURT(url));
    }

    public void addOutlink(String outlink, String anchor) {
        String outlinkSurt = WARCInformationParser.toSURT(outlink);
        if (outlinkSurt.trim().isEmpty())
            return;
        Outlink outlinkObj = new Outlink(outlinkSurt, outlink, anchor, this.timestamp, this.surt.get(0));
        this.outlinks.put(outlinkObj, outlinkObj);
    }

    public void addOutlink(Outlink outlink) {
        Outlink existing = outlinks.get(outlink);
        if (existing != null) {
            if (outlink.getCaptureDateStart().isBefore(existing.getCaptureDateStart())) {
                existing.setCaptureDateStart(outlink.getCaptureDateStart());
            }
            if (outlink.getCaptureDateEnd().isAfter(existing.getCaptureDateEnd())) {
                existing.setCaptureDateEnd(outlink.getCaptureDateEnd());
            }
            existing.incrementCount();
            return;
        }
        if (this.outlinks.size() > MAX_OUTLINKS)
            return;
        this.outlinks.put(outlink, outlink);
    }

    public void addInlink(Outlink inlink) {
        Outlink existing = inlinks.get(inlink);
        if (existing != null) {
            if (inlink.getCaptureDateStart().isBefore(existing.getCaptureDateStart())) {
                existing.setCaptureDateStart(inlink.getCaptureDateStart());
            }
            if (inlink.getCaptureDateEnd().isAfter(existing.getCaptureDateEnd())) {
                existing.setCaptureDateEnd(inlink.getCaptureDateEnd());
            }
            existing.incrementCount();
            return;
        }
        if (this.inlinks.size() > MAX_INLINKS)
            return;
        this.inlinks.put(inlink, inlink);
    }

    @Override
    public void write(DataOutput out) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        byte[] data = baos.toByteArray();
        int size = data.length;
        out.writeInt(size);
        out.write(data);
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        int size = in.readInt();
        byte[] data = new byte[size];
        in.readFully(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
            TextDocumentData other = (TextDocumentData) ois.readObject();

            this.collection = other.collection;
            this.warc = other.warc;
            this.warcOffset = other.warcOffset;
            this.mimeTypeDetected = other.mimeTypeDetected;
            this.mimeTypeReported = other.mimeTypeReported;
            this.title = other.title;
            this.urlTokens = other.urlTokens;
            this.timestamp = other.timestamp;

            this.timestampString = other.timestampString;
            this.url = other.url;
            this.surt = other.surt;
            this.host = other.host;
            this.content = other.content;
            this.outlinks = other.outlinks;
            this.inlinks = other.inlinks;
            this.digestContainer = other.digestContainer;
            this.digestContent = other.digestContent;
            this.metadata = other.metadata;

        } catch (ClassNotFoundException e) {
            System.err.println("Error reading TextDocumentData from Writable");
        }
    }

    public static TextDocumentData merge(TextDocumentData a, TextDocumentData b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        if (a == b) {
            return a;
        }
        TextDocumentData result = a.getTimestamp().isAfter(b.getTimestamp()) ? b : a;
        TextDocumentData other = a == result ? b : a;

        other.getCollection().forEach(result::addCollection);
        other.getTitle().forEach(result::addTitle);
        other.getURL().forEach(result::addURL);
        other.getContent().forEach(result::addContent);
        other.getOutlinks().keySet().forEach(result::addOutlink);
        other.getInlinks().keySet().forEach(result::addInlink);
        other.getMetadata().forEach(result::addMetadata);

        return result;


    }
}

