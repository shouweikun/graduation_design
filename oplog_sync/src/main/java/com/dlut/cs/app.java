package com.dlut.cs;

import com.dlut.cs.client.OplogScannerServer;
import com.dlut.cs.client.Scanner;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.BlockingQueue;

/**
 * Created by john_liu on 2017/3/17.
 */
public class app {
    private static final Logger logger = LoggerFactory.getLogger(app.class);
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("/Users/john_liu/graduation_design/oplog_sync/src/main/resources/META-INF/spring_config.xml");
        OplogScannerServer server     = context.getBean(OplogScannerServer.class);
        Scanner            scanner    = context.getBean(Scanner.class);
        BlockingQueue<DBObject> queue = context.getBean(BlockingQueue.class);
    }
}
