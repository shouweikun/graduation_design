package com.dlut.cs.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * Created by john_liu on 2017/3/31.
 */
public class ShutdownHook implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);
    private OplogScannerServer server;
    private Scanner            scanner;
    private Consumer           consumer;
    private BlockingQueue      queue;

    public ShutdownHook(OplogScannerServer server, Scanner scanner, Consumer consumer, BlockingQueue queue) {
        this.server = server;
        this.scanner = scanner;
        this.consumer = consumer;
        this.queue = queue;
    }

    public void run() {
        scanner.shutdown();
        //关闭资源
        while (!server.getScanner().getState().equals(Thread.State.TERMINATED)){
            try {
                logger.info("waiting for scanner shutdown!!!!!");
                Thread.sleep(500);
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        logger.info("scanner is terminated!!!");
        scanner.saveOffset();
        long[] offsetInfo = server.getScanner().getOffsetInfo();
        logger.info(String.format("scanner save offset successfully!!![%d,%d,%d]", offsetInfo[0], offsetInfo[1],
                offsetInfo[2]));
        // 暂时无法保证所有线程都已经处理完自己应该处理的数据
        // 需要用到submit 提交线程
        consumer.setForShutDownHook(true);
        logger.info("Consumer[for shutdown hook] will start!!!!!");
        while (!queue.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (Exception e) {
                logger.error("", e);
            }
            break;
        }
        logger.info("Consumer[for shutdown hook] is terminted!!!!!");
    }
}
