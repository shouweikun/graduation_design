package com.dlut.cs.client;

import java.lang.Thread.State;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dlut.cs.utils.OffsetHandler;
import com.dlut.cs.utils.OffsetResponse;
import com.dlut.cs.utils.ZookeeperOffsetHandler;
import com.mongodb.*;
import com.puhui.nbsp.cif.CifException;
import com.puhui.nbsp.cif.CifTransHead;
import com.puhui.nbsp.cif.commons.KafkaProducer;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.BSONTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by john_liu on 2017/3/20.
 */
@Component
public class Scanner implements Runnable {
    // need zk mongo resource
    private static final Logger logger = LoggerFactory.getLogger(Scanner.class);
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired()
    @Qualifier("clientTrans")
    ClientTrans clientTrans;
    @Autowired
    private RecourceManager recourceManager;
    private Thread thread;
    @Setter
    private BlockingQueue queue;
    @Autowired
    private AtomicBoolean running;
    private List<String> ignoreList;
    private List<String> concernList;
    private long sleepTime = 500l;
    private ExecutorService executor;
    @Getter
    private long[] offsetInfo   = new long[3];
    private KafkaProducer producer ;

    public String getName() {
        return thread.getName();
    }

    public State getState() {
        return thread.getState();
    }

    public boolean saveOffset() {
        recourceManager.offsetHandler().saveOffset(offsetInfo[0], offsetInfo[1], offsetInfo[2]);
        return true;
    }

    @PostConstruct
    public void init() {
        thread      = new Thread(this);
        thread.setName("Scanner Thread-" + new Random().nextInt());// set thread
        //name
        ignoreList  = collectProperty(recourceManager.getOplogconcernDbTableSkipList());
        concernList = collectProperty(recourceManager.getOplogConcernDbTableList());
        producer    = producer != null ? producer :new KafkaProducer(recourceManager.getKafkaConf(), true);
    }

    public void start() {

        executor = Executors.newFixedThreadPool(5);
        thread.start();
    }

    private List<String> collectProperty(String property) {
        List<String> list = new ArrayList<String>();
        if (!StringUtils.isBlank(property)) {
            for (String s : property.split(",")) {
                if (!StringUtils.isBlank(s)) {
                    list.add(s.trim());
                }
            }
        }
        return list;
    }

    public long getSleepTime() {
        if (sleepTime > 2000) {
            return 2000l;
        }
        sleepTime = (long) (sleepTime * 1.1);
        return sleepTime;
    }

    public void run() {
        //get oplog$main
        DBCollection oplogMain = recourceManager.mongo().getCollection(recourceManager.getMongoServerCollection());
        //第一次启动时，按当前时间开始读取
        long currentTimeStamp = System.currentTimeMillis() / 1000;

        ZookeeperOffsetHandler zookeeper = recourceManager.offsetHandler();
        OffsetResponse offset = zookeeper.getOffset();
        if (!offset.isSuccess()) {
            //链式
            offset = new OffsetResponse().setTimestamp(0).setIncrement(0);
            zookeeper.saveStartingPosition(currentTimeStamp);
        }

        while (running.get() && oplogMain != null) {
            try {

                offset = zookeeper.getOffset();

                long timeStamp = (int) offset.getTimestamp();
                BSONTimestamp ts = new BSONTimestamp((int) timeStamp, offset.getIncrement());
                final BasicDBObject query = new BasicDBObject();
                query.append("ts", new BasicDBObject(QueryOperators.GT, ts));
                DBCursor cursor = oplogMain.find(query).sort(new BasicDBObject("natural", 1));
                cursor = cursor.addOption(Bytes.QUERYOPTION_OPLOGREPLAY);
                try {
                    long result[] = doScan(cursor, offset);
                    logger.info(String.format("scanning oplog [%d] records", result[0]));
                    if (result[0] == 0) {

                            try {
                                logger.info("scan:" + offset.getTimestamp() + ":" + offset.getIncrement());
                                Thread.sleep(getSleepTime());
                            } catch (InterruptedException e) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                        else {
                         sleepTime = 500;
                    }
                    zookeeper.saveOffset(result[1],result[2],result[3]);
                    cursor.close();
                    } catch (CifException e) {
                    logger.error(e.getMsgDes(), e);
                }
                } catch (Exception e) {
                logger.error("==>Scanner run method Exception", e);
            }
        }
    }

    public void setCurrentTimeStamp(Long ts, long increment, long delayTime) {
        offsetInfo[0] = ts;
        offsetInfo[1] = increment;
        offsetInfo[2] = delayTime;
    }

    public long[] doScan(DBCursor cursor, OffsetResponse offsetInfo) {
        long res[] = new long[4];
        res[0] = 0;
        res[1] = offsetInfo.getTimestamp();
        res[2] = offsetInfo.getIncrement();

        while (cursor != null && cursor.hasNext()) {
            final DBObject obj = cursor.next();
            BSONTimestamp o = (BSONTimestamp) obj.get("ts");
            final Object timeObject = o.getTime();
            final long ts = 1000 * Long.parseLong(timeObject.toString().trim());
            System.out.println(isConcerned(obj));
            if (isConcerned(obj)) {
                if(executor==null) {
                    executor = Executors.newFixedThreadPool(5);
                }
                executor.execute(new Runnable() {

                    public void run() {
                        processMessage(obj, timeObject, ts);
                    }

                });
                res[0]++;
            }
            long delay = System.currentTimeMillis() - (o.getTime() * 1000l);
            res[1] = o.getTime();
            res[2] = o.getInc();
            res[3] = delay;


            setCurrentTimeStamp(res[1], res[2], res[3]);
            offsetInfo.setTimestamp(res[1]);
            offsetInfo.setIncrement((int) res[2]);
            if (res[0] > 0 && res[0] % 1000 == 0) {
                Date date = new Date();
                Date date1 = new Date(o.getTime() * 1000l);
                logger.info(String.format("scanned %d record(s) whit starting position[%d,%d]", res[0],
                        offsetInfo.getTimestamp(), offsetInfo.getIncrement()));
                logger.info(String.format("current time[%s],oplog timestamp[%s],time delay[%d]ms", date.toString(),
                        date1.toString(), delay));
                saveOffset();
            }
        }
         return      res;
    }

    private boolean isConcerned(DBObject obj) {
        Object opObj = obj.get("op");
        Object nsObj = obj.get("ns");
        if (nsObj == null  || nsObj.toString().indexOf(".") < 0) {
            return false;
        } else if (!inConcernedList(nsObj.toString()) || inIgnoreList(nsObj.toString())) {
            return false;
        }
        if (opObj == null || !opObj.toString().matches(Constants.OPLOG_OP_PATTERN)) {
            return false;
        }
        return true;

    }

    private boolean inConcernedList(String ns) {
        String database = ns.split("[.]")[0];
        if (concernList.isEmpty()  && !concernList.contains(ns.toString())) {
            return false;

        }
        return true;
    }

    private boolean inIgnoreList(String ns) {
        String database = ns.split("[.]")[0];
        if (!ignoreList.isEmpty() && (ignoreList.contains(database) || ignoreList.contains(ns))) {
            return true;
        }
        return false;
    }

    public String processMessage(DBObject obj,Object timeObject,long ts) throws CifException {
        System.out.println("processing message");
        String Json = JSONObject.toJSONString(obj, SerializerFeature.WriteMapNullValue);
        String channel = "";
        try {
            CifTransHead transJson = clientTrans.transBean(Json, ts);
            if (transJson != null && StringUtils.isNotBlank(transJson.getJsonBody()) &&! "null".equalsIgnoreCase(transJson.getJsonBody())) {
                if(recourceManager.isNeedWrite2EsHbase()){
                queue.put(transJson);
                }
                String kfkMessage = String.format("%s@%s@%s\t%s", transJson.getDbName(), transJson.getTableName(), format.format(new Date()), transJson.getJsonBody());
                try {
                    if (recourceManager.isNeedSendOplog2Offline())
                        producer.sendKfkMessage(recourceManager.getOplog2OffLineTopic() + renameDBName(transJson.getDbName()),
                                "" + System.currentTimeMillis(), kfkMessage);
                } catch (Exception e) {
                    logger.error(e.getClass().getName(), e);
                    logger.warn("send message to topic[{}] fail,data[{}]", recourceManager.getOplog2OffLineTopic(), kfkMessage);
                }
            }
            channel = transJson.getDbName();
        } catch (CifException e) {
            throw new CifException("queue中添加元素异常", e);
        } catch (InterruptedException e) {
            throw new CifException("queue中添加元素异常", e);
        }
        return channel;
    }
    public static String renameDBName(String topicName) {
        if (org.apache.commons.lang.StringUtils.isBlank(topicName)) {
            return null;
        }
        if (topicName.contains("_")) {
            String d = "";
            char[] c = topicName.toCharArray();
            if (Character.isLowerCase(c[0]))
                c[0] -= 32;
            for (int i = 0; i < c.length; i++) {
                if (c[i] == '_') {
                    if (Character.isLowerCase(c[i + 1]))
                        c[i + 1] -= 32;
                    continue;
                }
                d = d + c[i];
            }
            return d;
        } else {
            //return topicName;
            return topicName.substring(0, 1).toUpperCase() + topicName.substring(1);
        }
    }

    public void shutdown() {
        running.set(false);
    }
}
