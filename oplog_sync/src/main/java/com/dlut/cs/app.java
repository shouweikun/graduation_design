package com.dlut.cs;

import com.dlut.cs.client.Consumer;
import com.dlut.cs.client.OplogScannerServer;
import com.dlut.cs.client.Scanner;
import com.dlut.cs.client.ShutdownHook;
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
        String[] locations = {"META-INF/spring_config.xml"};
        ApplicationContext context = new ClassPathXmlApplicationContext(locations);
        OplogScannerServer      server     = context.getBean(OplogScannerServer.class);
        Scanner                 scanner    = context.getBean(Scanner.class);
        Consumer                consumer   = context.getBean(Consumer.class);
        BlockingQueue<DBObject> queue      = context.getBean(BlockingQueue.class);
        consumer.setQueue(queue);
        scanner.setQueue(queue);
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(server,scanner,consumer,queue)));
        server.start();
    }
}
