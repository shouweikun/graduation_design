package com.dlut.cs.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Created by john_liu(NEIGHBORHOOD) on 2017/3/19.
 * 保存oplog偏移量的工具，使用ZooKeeper来记录
 * ゼンちゃん（Zen酱）のこと、大好きだ
 */
public class ZookeeperOffsetHandler implements OffsetHandler{
      SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
      private static final Logger logger = Logger.getLogger(ZookeeperOffsetHandler.class);
      private String zkAddress;
      private String dataPath;
      private String oplogSyncID;
      private String offsetInfoPath;
      private String startingPositionPath;
      private ZkClient zkClient;
      private long offsetInfo[] = new long[2];

    public ZookeeperOffsetHandler(String zkAddress, String dataPath, String oplogSyncID) {
        this.zkAddress = zkAddress;
        this.dataPath = dataPath;
        this.oplogSyncID = oplogSyncID;
        offsetInfo[0] = 1;
        offsetInfo[1] = 0;
    }
    public void init()
        {
            dataPath =dataPath.endsWith("/")? dataPath.substring(0,dataPath.indexOf("/")) : dataPath;
            offsetInfoPath = dataPath + "/" + oplogSyncID +
                    "/currentOffset";
            startingPositionPath = dataPath + "/" + oplogSyncID
                    + "/startingPosition";
            initZK();

        }

        public ZkClient getZkClient()
        {
            if(this.zkClient == null)
            {
                 zkClient= new ZkClient(zkAddress);
                zkClient.setZkSerializer(new BytesPushThroughSerializer());
            }
        return zkClient;
        }

        public void createZknode(String dataPath,String data,ZkClient zkClient)
        {
            int index = dataPath.indexOf("/",1);
            int countdown = dataPath.split("/").length;
            while (index >= 1 && index < dataPath.length())
            {
//                if(countdown <= 3 ){
                System.out.println(dataPath.substring(0,index)+!zkClient.exists(dataPath.substring(0,index)));
                if(!zkClient.exists(dataPath.substring(0,index)))
                {
                    zkClient.createPersistent(dataPath.substring(0, index), "".getBytes());
                }
                index = dataPath.indexOf("/",index + 1);

            }
             zkClient.createPersistent(dataPath,data.getBytes());
//            countdown --;
//            }
        }

        public void initZK()
        {
            try{
                System.out.println(dataPath);
                if(getZkClient().exists(dataPath))
                {
                    Date date = new Date();
                    createZknode(dataPath,date.getTime() +":0:0:" + format.format(date),zkClient);
                }
            } catch (Exception e)
            {
                logger.error("exception",e);
            }
        }

    public OffsetResponse getOffset() {
        OffsetResponse response = new OffsetResponse();
        if(zkClient == null)
        {
            initZK();
        }
        String data = null;
        try
        {
            if(!zkClient.exists(offsetInfoPath))
            {   System.out.println(offsetInfoPath);
                long currentTimeStamp = System.currentTimeMillis()/1000;
                createZknode(offsetInfoPath,currentTimeStamp+":0:0",zkClient);
            }
            data = new String((byte[])zkClient.readData(offsetInfoPath));
        } catch (Exception e)
        {
            response.setSuccess(false);
            response.setE(e);
            logger.error("Exception",e);
        }
        if(data != null)
        {
            String value[] = data.split(":");
            String tsOffset = value[0];
            String increment =value[1];
            response.setTimestamp(Long.parseLong(tsOffset));
            response.setIncrement(Integer.parseInt(increment));
            response.setSuccess(true);
            response.setE(null);

        }
        return response;
    }

    public boolean saveOffset(long tsOffset, long increment, long delayTime) {

        offsetInfo[0] = tsOffset;
        offsetInfo[1] = increment;
        String data = String.format("%d:%d:%d:%s",tsOffset,increment,delayTime,format.format(new Date()));
        if(zkClient == null)
        {
            initZK();
        }
        if(!zkClient.exists(offsetInfoPath))
        {
            createZknode(offsetInfoPath,data,zkClient);
        }

        zkClient.writeData(offsetInfoPath,data.getBytes());

        return true;
    }

    public boolean saveStartingPosition(long tsOffset) {

        String data = "" + tsOffset;
        if (zkClient == null)
        {
            initZK();
        }
        try
        {
            if (zkClient.exists(startingPositionPath))
            {
                createZknode(startingPositionPath,data,zkClient);

            }
            zkClient.writeData(offsetInfoPath,data.getBytes());
        }catch (Exception e)
        {
            logger.error("Exception",e);
            return false;
        }
        return true;
    }

    public void close()
    {
        if(zkClient!= null)
            zkClient.close();
    }
}
