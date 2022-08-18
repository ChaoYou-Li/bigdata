package pf.bluemoon.com.hadoop;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pf.bluemoon.com.hadoop.client.HadoopClient;
import pf.bluemoon.com.hadoop.combine.CombineDriver;
import pf.bluemoon.com.hadoop.combiner.CombinerDriver;
import pf.bluemoon.com.hadoop.comparable.ComparableDriver;
import pf.bluemoon.com.hadoop.etl.ETLDriver;
import pf.bluemoon.com.hadoop.reducejoin.JoinDriver;
import pf.bluemoon.com.hadoop.output.LogDriver;
import pf.bluemoon.com.hadoop.partitition.PartitionDriver;
import pf.bluemoon.com.hadoop.serializable.FlowDriver;
import pf.bluemoon.com.hadoop.worldcount.HelloDriver;

import java.io.IOException;
import java.net.URISyntaxException;

@SpringBootTest
class HadoopApplicationTests {

    @Test
    void find() throws IOException {
        FileSystem fs = HadoopClient.initClient();
        fs.mkdirs(new Path("/output"));
    }

    @Test
    void contextLoads() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "/input/hello",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\hello3"
        };
        HelloDriver.drive(paths);
    }

    @Test
    void combineTest(){
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputcombinetextinputformat",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\combine"
        };
        CombineDriver.drive(paths);
    }

    @Test
    void partitionTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputflow",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\partition"
        };
        PartitionDriver.drive(paths);
    }

    @Test
    void flowTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputflow",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\flow"
        };
        FlowDriver.drive(paths);
    }

    @Test
    void comparableTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputflow",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\comparable"
        };
        ComparableDriver.drive(paths);
    }

    @Test
    void combinerTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputflow",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\combiner"
        };
        CombinerDriver.drive(paths);
    }

    @Test
    void outputTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\input\\inputoutputformat",
                "D:\\workspace\\idea\\springboot\\hadoop\\src\\main\\resources\\output\\out"
        };
        LogDriver.drive(paths);
    }

    @Test
    void joinTest() throws InterruptedException, IOException, ClassNotFoundException {
        String[] paths = {
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\input\\inputtable",
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\output\\join"
        };
        JoinDriver.drive(paths);
    }

    @Test
    void join2Test() throws InterruptedException, IOException, ClassNotFoundException, URISyntaxException {
        String[] paths = {
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\input\\inputtable2",
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\output\\join2"
        };
        pf.bluemoon.com.hadoop.mapjoin.JoinDriver.drive(paths);
    }

    @Test
    void etlTest() throws InterruptedException, IOException, ClassNotFoundException, URISyntaxException {
        String[] paths = {
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\input\\inputlog",
                "C:\\workspace\\idea\\springboot\\hadoop-demo\\src\\main\\resources\\output\\etl"
        };
        ETLDriver.drive(paths);
    }

    @Test
    void zipTest() throws InterruptedException, IOException, ClassNotFoundException, URISyntaxException {
        String[] paths = {
                "D:\\workspace\\idea\\springboot\\bigdata\\hadoop\\src\\main\\resources\\input\\inputlog",
                "D:\\workspace\\idea\\springboot\\bigdata\\hadoop\\src\\main\\resources\\output\\zip"
        };
        pf.bluemoon.com.hadoop.mapzip.ETLDriver.drive(paths);
    }


}
