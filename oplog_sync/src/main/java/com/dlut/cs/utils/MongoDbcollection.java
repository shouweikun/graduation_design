package com.dlut.cs.utils;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by john_liu on 2017/3/17.
 */
public class MongoDbcollection {

        private static final Logger logger = LoggerFactory.getLogger(MongoDbcollection.class);
       //
        String userName;
        String password;
        String database;
        String addressList;
        private Datastore datastore;
        private Morphia morphia;
        private MongoClient client;
        private DB db;

        public MongoDbcollection(String username, String password, String credentialsDBName, String addresses) {
            this.userName = username;
            this.password = password;
            this.database = credentialsDBName;
            this.addressList = addresses;
        }

        public DB use(String database) {
            if (client == null) {
                initMongo();
            }
            datastore = morphia.createDatastore(client, database);
            db = client.getDB(database);
            return db;
        }

        private void initMongo() {
            List<ServerAddress> hosts = new ArrayList<ServerAddress>();
            for (String address : addressList.split(",")) {
                String host = address.split(":")[0];
                int port = Integer.parseInt(address.split(":")[1]);
                hosts.add(new ServerAddress(host, port));
            }
            morphia = new Morphia();
            List<MongoCredential> mongoCredentials = null;
            if (!StringUtils.isBlank(userName)) {
                mongoCredentials = new ArrayList<MongoCredential>();
                mongoCredentials.add(MongoCredential.createScramSha1Credential(userName, database, password.toCharArray()));
                client = new MongoClient(hosts, mongoCredentials, getConfOptions());
            }else{
                client = new MongoClient(hosts, getConfOptions());
            }

            datastore = morphia.createDatastore(client, database);
            logger.info("connect to moongodb[local] success!!!!!!!!");
        }

        private static MongoClientOptions getConfOptions() {
            MongoClientOptions.Builder build = new MongoClientOptions.Builder();
            // 设置活跃连接数 默认为10
            build.connectionsPerHost(500);
            // 连接超时时间(毫秒)，默认为10000
            build.connectTimeout(60000);
            // 线程等待连接可用的最大时间(毫秒)，默认为120000
            build.maxWaitTime(120000);
            build.socketTimeout(50000);
            MongoClientOptions mongoClientOptions = build.build();
            return mongoClientOptions;
        }

    }

