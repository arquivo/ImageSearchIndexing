package pt.arquivo.imagesearch.indexing;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Tool;
import org.junit.Before;
import org.junit.Test;
import java.io.*;
import java.net.URL;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class HadoopFullPipelineTest {
    private Configuration conf;

    @Before
    public void setup() throws IOException {
        conf = new Configuration();
        conf.set("fs.default.name", "file:///");
        conf.set("mapred.job.tracker", "local");
    }

    private void runMapReduceJob(Tool job, Configuration conf, String[] args) throws Exception {
        job.setConf(conf);
        int output = job.run(args);
        assert(output == 0);
    }

    @Test
    public void testImageSearchIndexingWorkflow() throws Exception {

        String logLevel = System.getenv("INDEXING_LOG_LEVEL");
        if (logLevel != null) {
            org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.toLevel(logLevel));
        } else {
            org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.ERROR);
        }

        ClassLoader classLoader = getClass().getClassLoader();

        URL hdfsArcsPath = classLoader.getResource("FullOfflinePipelineTestWARCS.txt");
        
        assertNotNull(hdfsArcsPath);

        String path = hdfsArcsPath.getPath();
        String collection = "Teste";

        File tempFile = File.createTempFile("teste", "teste");
        tempFile.deleteOnExit();

        PrintWriter pw = new PrintWriter(tempFile);

        BufferedReader br = new BufferedReader(new FileReader(path));
        for (String line; (line = br.readLine()) != null; ) {
            if (!line.trim().isEmpty()) {
                URL warcURL = classLoader.getResource(line.trim());
                assertNotNull(warcURL);
                String warcPath = "file://" + warcURL.getPath();
                pw.println(warcPath);
            }
        }
        pw.close();

        Path output = new Path("target/output");
        FileSystem fs = FileSystem.getLocal(conf);

        fs.mkdirs(new Path("target/output"));
        fs.mkdirs(new Path("target/outputI"));

        String[] args = new String[]{tempFile.getPath(), collection, "1", "1", "false", "target/outputI", "/tmp"};

        runMapReduceJob(new ImageIndexerWithDupsJob(), conf, args);

        args = new String[]{collection, "1", "COMPACT", "target/outputI", "target/output"};

        runMapReduceJob(new DupDigestMergerJob(), conf, args);

        URL warcURL = classLoader.getResource("outputs/HadoopFullPipelineTest.jsonl");
        assertNotNull(warcURL);
        final File expected = new File(warcURL.getPath());

        Path outputHDFS = new Path("target/output/part-r-00000");
        InputStream in = null;
        String text = "";
        try {
            in = fs.open(outputHDFS);
            text = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } finally {
            IOUtils.closeStream(in);
        }
        String expectedText = FileUtils.readFileToString(expected);
        assertEquals(expectedText, text);
    }
}