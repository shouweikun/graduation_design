package com.dlut.cs.client;

import com.dlut.cs.utils.MongoDbcollection;
import com.dlut.cs.utils.ZookeeperOffsetHandler;
import com.mongodb.DB;
import com.puhui.nbsp.cif.commons.ESUtils;
import com.puhui.nbsp.cif.commons.HBaseUtils;
import com.puhui.nbsp.cif.commons.JedisUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;


import java.io.IOException;

/**
 * Created by john_liu on 2017/3/20.
 */
@Service
public class RecourceManager {
    @Value("${mongo.server.username}")
    @Getter
    String mongoUserName;
    @Value("${mongo.server.password}")
    @Getter
    String mongoPassword;
    @Value("${mongo.server.default.auth.db}")
    @Getter
    String mongoDatabase;
    @Value("${mongo.server.address}")
    @Getter
    String mongoAddressList;
    @Value("${oplog.concern.db.table.list}")
    @Getter
    String oplogConcernDbTableList;
    @Value("${oplog.concern.db.table.skip.list}")
    @Getter
    String oplogconcernDbTableSkipList;
    @Value("${mongo.server.collection}")
    @Getter
    String mongoServerCollection;
    @Value("${zk.address}")
    @Getter
    private String zkAddress;
    @Value("${zk.path}")
    @Getter
    private String zkPath;
    @Value("${zk.oplog.sync.id}")
    @Getter
    private String zkOplogsyncId;
    @Value("${hbase.quorum}")
    @Getter
    private String hbaseQuorum;
    @Value("${hbase.port}")
    @Getter
    private String hbasePort;
    @Value("${es.esClusterName}")
    @Getter
    private String esClusterName;
    @Value("${es.esIp}")
    @Getter
    private String esIp;
    @Value("${redis.password}")
    @Getter
    private String redisPassword;
    @Value("${redis.ip}")
    @Getter
    private String redisIp;
    @Value("${redis.port}")
    @Getter
    private String redisPort;
    @Value("${redis.expire.seconds}")
    @Getter
    private int redisExpireSeconds;
    @Value("${oplog.cache.queue.size}")
    @Getter
    private int cacheQueueSize;
    @Value("${oplog.consumer.count}")
    @Getter
    private int consumerCount;
    @Value("${save.hbase.fail.kafka.topic}")
    @Getter
    private String hbaseFailTopic;
    @Value("${es.encrypt.data.topic}")
    @Getter
    private String esEncryptDataTopic;
    @Value("${oplog.offline.kafka.topic}")
    @Getter
    private String oplog2OffLineTopic;
    @Value("${offline.need.oplog.flag}")
    @Getter
    private boolean needSendOplog2Offline;
    @Value("${kafka.conf}")
    @Getter
    private String kafkaConf;

    public RecourceManager() {}

        private DB localDatabase;

        private MongoDbcollection mdc;

        public MongoDbcollection mdc()
        {
             return (mdc == null)? mdc  = new MongoDbcollection(mongoUserName,mongoPassword,mongoDatabase,mongoAddressList) :mdc;
        }
        public DB mongo()
            {
             return (localDatabase == null) ?  new MongoDbcollection(mongoUserName, mongoPassword, mongoDatabase, mongoAddressList).use("local") :localDatabase;
            }
        private ZookeeperOffsetHandler offset;

        public ZookeeperOffsetHandler offsetHandler()
        { // offer zk init&reconnect
            if(offset == null)
            {
                offset = new ZookeeperOffsetHandler(zkAddress,zkPath,zkOplogsyncId);
                offset.init();
            }
            return offset;
        }
          // selectable part
        //************ *************** ***********

     private HBaseUtils hbase;

    public HBaseUtils hbase() throws IOException {
        String host = hbaseQuorum;
        String port = hbasePort;
        return (hbase == null) ? hbase = HBaseUtils.getInstance(host, port) : hbase;
    }

    public HBaseUtils getHbase() {
        return hbase;
    }

    public void setHbase(HBaseUtils h) {
        hbase = h;
    }

    private ESUtils es;

    public ESUtils es() {
        return (es == null) ? es = ESUtils.getInstance(esClusterName, esIp) : es;
    }

    private Jedis redis;

    public Jedis redis() {
        return (redis == null)
                ? redis = JedisUtil.getPool(redisIp, Integer.parseInt(redisPort), redisPassword).getResource() : redis;
    }


}
