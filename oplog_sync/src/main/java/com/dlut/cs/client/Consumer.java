package com.dlut.cs.client;

import com.puhui.nbsp.cif.CifDataParseFactory;
import com.puhui.nbsp.cif.CifTransHead;
import com.puhui.nbsp.cif.TransUtils;
import com.puhui.nbsp.cif.commons.KafkaProducer;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    @Setter
    private  BlockingQueue       queue;
    @Value("${CustomerThreadPool}")
    private  String              cusumerThreadPool;
    @Autowired
    private  RecourceManager     res;
    private  ExecutorService     reads;
    private  boolean             running          = true;
    @Setter
    private  boolean             forShutDownHook  = false;
    @Autowired
    private  CifDataParseFactory transFactory;
    @Autowired
    private  TransUtils          transUtils;

    private  KafkaProducer       producer;
    @PostConstruct
    public void init() {
        int readThreadPool = 5;
        try {
            readThreadPool = Integer.parseInt(cusumerThreadPool);
        } catch (Exception e) {
            logger.error("",e);
        }
        producer = producer!=null?producer:new KafkaProducer(res.getKafkaConf(),false);
        reads = Executors.newFixedThreadPool(readThreadPool);
    }

    public void execute() {
        int lineCount = 0;
        while (running) {
            if (forShutDownHook && queue.isEmpty()) {
                break;
            }
            CifTransHead client = (CifTransHead) queue.poll();
            // 每间隔一段时间 打印现在queue里还剩多少待处理数据
            if (client != null) {
                lineCount += 1;
                // 交给多个线程去处理
                reads.execute(new SaveDataTask(client,res,transFactory,transUtils,producer));
                if (lineCount % 50 == 0) {
                    lineCount = 0;
                    try {
                        logger.info(
                                "==>customer thread queue size=" + queue.size() + " oplog ts=" + client.getTsTime());
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("",e);
                }
            }
        }
    }
}