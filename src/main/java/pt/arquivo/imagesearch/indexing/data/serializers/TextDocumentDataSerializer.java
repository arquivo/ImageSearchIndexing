package pt.arquivo.imagesearch.indexing.data.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import pt.arquivo.imagesearch.indexing.data.TextDocumentData;
import java.lang.reflect.Type;

/**
 * Configures JsonSerializable to export the object in the desired JSON format
 * Used in the COMPACT export format
 */
public class TextDocumentDataSerializer implements JsonSerializer<TextDocumentData> {

    /**
     * Converts the object into a JSON ready for writing
     *
     * @param src object to export
     * @param typeOfSrc (unused)
     * @param context Hadoop context
     * @return JsonElement ready for export
     */
    @Override
    public JsonElement serialize(TextDocumentData src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        //obj.addProperty("type", "document");

        obj.addProperty("id", src.getId());

        obj.addProperty("digestContainer", src.getDigestContainer());
        obj.addProperty("url", src.getUrl());
        obj.addProperty("date", src.getTimestampFormatted());
        obj.addProperty("tstamp", src.getTimestampString());
        if (!src.getTitle().isEmpty())
            obj.addProperty("title", src.getTitle());
        obj.addProperty("type", src.getMimeTypeDetected());
        obj.addProperty("typeReported", src.getMimeTypeReported());
        if (!src.getContent().isEmpty())
            obj.addProperty("content", src.getContent());
        obj.addProperty("urlTokens", src.getUrlTokens());
        obj.addProperty("host", src.getHost());
        if (!src.getMetadata().isEmpty())
            obj.addProperty("metadata", src.getMetadata());


        //obj.addProperty("warc", src.getWarc());
        //obj.addProperty("warcOffset", src.getWarcOffset());
        //obj.addProperty("surt", src.getSurt());
        //obj.addProperty("protocol", src.getProtocol());
        //obj.addProperty("urlHash", src.getURLHash());
        obj.addProperty("collection", src.getCollection());
        //obj.add("outlinks", context.serialize(src.getOutlinks()));
        
        //obj.addProperty("safe", 0);
        //obj.addProperty("spam", 0);
        //obj.addProperty("blocked", 0);

        return obj;
    }
}
