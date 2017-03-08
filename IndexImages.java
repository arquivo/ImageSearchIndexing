import hadoopImageParser.ImageSearchResult;
import hadoopImageParser.ImageParse;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import java.io.IOException;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
 
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.io.ArchiveRecord;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


class Map extends Mapper<LongWritable, Text, Text, Text> {

    String collectionName;

    @Override
    public void configure(JobConf job) {
        super.configure(job);
        collectionName = job.get("collection", "notSet");
    }


    public static void parseImagesFromHtmlRecord(ARCRecord record, Context context) throws IOException{
        OutputStream output = new OutputStream()
        {
            private StringBuilder string = new StringBuilder();
            @Override
            public void write(int b) throws IOException {
                this.string.append((char) b );
            }

            //Netbeans IDE automatically overrides this toString()
            public String toString(){
                return this.string.toString();
            }
        };                    
        record.dump(output);
        String pageHTML = output.toString();
        try{
            Document doc = Jsoup.parse(pageHTML);
            Elements imgs = doc.getElementsByTag("img");
            for(Element el: imgs){
                JSONObject obj = new JSONObject();      
                String originalURL = record.getHeader().getUrl();
                String imgSrc = el.attr("src");

                if(!imgSrc.startsWith("http")){
                    /*Relative Path lets reconstruct full path*/
                    /*TODO:: check how to reconstruc relative paths*/
                    if(imgSrc.startsWith("/")){ /* Relative path starting in the host*/
                            URL uri = new URL(originalURL);
                            String domain = uri.getHost();
                            String protocol = uri.getProtocol();
                            imgSrc = protocol+ "://"+ domain + imgSrc;
                    }
                    else{
                        imgSrc= originalURL + "/" + imgSrc;
                    }
                }
                if(imgSrc.length() > 10000 || originalURL.length() > 10000){
                    System.out.println("URL of image too big ");
                    continue;
                }/*Maximum size for SOLR index is 10 000*/
                
                String timestamp = record.getMetaData().getDate();
                if (timestamp == null || timestamp.equals("")){
                    System.out.println("Null Timestamp");
                }
                if (imgSrc == null || imgSrc.equals("")){
                    System.out.println("Null imgSrc");
                }



                ImageSearchResult imgResult = ImageParse.getPropImage("http://preprod.arquivo.pt/wayback/"+timestamp+"/"+imgSrc);
                if ( imgResult == null ){
                    System.out.println("null image");
                    continue;
                }

                obj.put( "imgWidth", imgResult.getWidth( ) ); /*To be replaced with Real Width of Image*/
                obj.put( "imgHeight", imgResult.getHeight( ) ); /*To be replaced with Real Height of Image*/
                //obj.put( "digest" , imgResult.getDigest( ) );
                obj.put( "imgSrc", imgSrc); /*The URL of the Image*/
                if(el.attr("title").length() > 9999){
                    obj.put( "imgTitle", el.attr("title").substring(0,10000) );
                }
                else{
                    obj.put( "imgTitle", el.attr("title") );
                }
                if(el.attr("alt").length() > 9999){
                    obj.put( "imgAlt", el.attr("alt").substring(0,10000) );
                }
                else{
                    obj.put( "imgAlt", el.attr("alt"));
                }
                //obj.put("imgWidth", el.attr("width")); /*To be replaced with Real Width of Image*/
                //obj.put("imgHeight", el.attr("height")); /*To be replaced with Real Height of Image*/
                obj.put("timestamp", timestamp);
                obj.put("originalURL", originalURL); /*The URL of the Archived page*/
                obj.put("collection", collectionName);
                
                context.write( new Text (obj.toJSONString()),null);
            }
        }catch (Exception e){
            System.out.println("Something failed JSOUP parsing");
            e.printStackTrace();
        }
      
    }



	public void map(LongWritable key, Text value, Context context)
			throws IOException, InterruptedException {
        ARCReader reader = null;
        try {
            int records = 0;
            int errors = 0;
 
            reader = ARCReaderFactory.get(value.toString());


            for (Iterator<ArchiveRecord> ii = reader.iterator(); ii.hasNext();) {
                    ARCRecord record = (ARCRecord)ii.next();

                    if(record.getMetaData().getMimetype().contains("html"))
                        parseImagesFromHtmlRecord(record, context);
                    ++records;
                    if (record.hasErrors()) {
                        errors += record.getErrors().size();
                    }                        
            }
            System.out.println("--------------");
            System.out.println("       Records: " + records);
            System.out.println("        Errors: " + errors);
            /*reader.close();*/
            		// Do what ever you want here
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally{
            if(reader!=null){
                reader.close();
            }
        }				
	}

}


public class IndexImages 
{
    public static void main( String[] args ) throws IOException, ClassNotFoundException, InterruptedException
    {
    	Configuration conf = new Configuration();
	Job job = new Job(conf, "Mapper_Only_Job");

	job.setJarByClass(IndexImages.class);
	job.setMapperClass(Map.class);
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(Text.class);
	job.setJobName(args[2]);
	conf.set("collection", args[2]);

    job.setInputFormatClass(NLineInputFormat.class);
    NLineInputFormat.addInputPath(job, new Path(args[0]));
    job.getConfiguration().setInt("mapreduce.input.lineinputformat.linespermap", 1);

	job.setOutputFormatClass(TextOutputFormat.class);
		
	// Sets reducer tasks to 0
	job.setNumReduceTasks(0);

	FileOutputFormat.setOutputPath(job, new Path(args[1]));

	boolean result = job.waitForCompletion(true);

	System.exit(result ? 0 : 1);
    }
}