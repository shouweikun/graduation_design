package utils;

import com.dlut.cs.utils.ZookeeperOffsetHandler;
import org.I0Itec.zkclient.ZkClient;
import org.junit.Test;

/**
 * Created by john_liu on 2017/5/7.
 */
public class ZookeeperOffsetHandlerTest {

    ZookeeperOffsetHandler zookeeperOffsetHandler;
    String zkaddress           = "localhost:2181";
    String datapath            = "/Users/john_liu/opt/kafka_2.11-0.8.2.2/data/zookeeper";
    String op_syncid           = "test";
    @Test
    public void test(){
        zookeeperOffsetHandler = new ZookeeperOffsetHandler(zkaddress,datapath,op_syncid);
        ZkClient zkClient     = zookeeperOffsetHandler.getZkClient();
       // zkClient.createPersistent("/dd","".getBytes());
        //zkClient.createPersistent("/Users/john_liu/opt/kafka_2.11-0.8.2.2/data/zookeeper",true);
        System.out.println(zkClient.exists("/Users"));
        zookeeperOffsetHandler.createZknode(datapath,"",zookeeperOffsetHandler.getZkClient());
    }
}
