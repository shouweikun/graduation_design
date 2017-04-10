package com.dlut.cs.client;



import com.alibaba.fastjson.JSON;

import com.alibaba.fastjson.JSONObject;
import com.puhui.aes.AesEncryptionUtil;
import com.puhui.nbsp.cif.*;
import com.puhui.nbsp.cif.commons.HBaseUtils;
import com.puhui.nbsp.cif.commons.KafkaProducer;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Random;


/**
 * Created by john_liu on 2017/3/26.
 */
public class SaveDataTask implements Runnable{
    private static final String NeedEncryptFieldNames[] = new String[] { };//此处填写待处理编码的字段
    private static final Logger logger                  = LoggerFactory.getLogger(SaveDataTask.class);
    private static final Logger errDataLogger           = LoggerFactory.getLogger("saveFaildData");
    private static final Logger dataBackLogger          = LoggerFactory.getLogger("backupData");
    //日志记录

    private CifTransHead         data;
    private RecourceManager      resourceManager;
    private CifDataParseFactory  transFactory;
    private TransUtils           transUtils;
    private Thread               thread;
    private KafkaProducer        producer;

    //coustructor
    public SaveDataTask(CifTransHead data, RecourceManager resourceManager, CifDataParseFactory transFactory, TransUtils transUtils, KafkaProducer producer) {
        this.data              = data;
        this.resourceManager   = resourceManager;
        this.transFactory      = transFactory;
        this.transUtils        = transUtils;
        this.producer          = producer;
        thread.setName("Oplog-Customer-Thread-"+ new Random().nextInt());
    }

    public void run() {

        String     Json           = JSON.toJSONString(data);
        CifData    processData    = null;
        try {
                   processData    = transFactory.trans(Json);

        } catch (CifException e)
        {
            errDataLogger.error(Json);
            logger.error("Exception",e);
            return;
        }
        dataBackLogger.info(Json);
        long       hbaseBefore    = System.currentTimeMillis();
        boolean    flag           = false;
        HBaseUtils hbase          = null;
        try {
                   hbase          = resourceManager.hbase();
        } catch (IOException e) {
            logger.error("connection to HBase Error HBaseQuorum=" + resourceManager.getHbaseQuorum() + " | HBasePort=" + resourceManager.getHbasePort(), e);
        boolean    isNull         = resourceManager.getHbase()== null;
        if (isNull) {
            try {
                  hbase           = resourceManager.hbase();
            } catch (IOException e1) {

            }
        }
        }
        try {
        long     s               = System.currentTimeMillis();
                 flag            = transUtils.saveToHBase(hbase,processData);
        long     e               = System.currentTimeMillis();
        } catch (IOException e) {
            if (e instanceof RetriesExhaustedWithDetailsException) {
                RetriesExhaustedWithDetailsException re = (RetriesExhaustedWithDetailsException) e;
                boolean isDataProblems = re.mayHaveClusterIssues(); // true: input error problems
                if (isDataProblems) {
                    logger.error("==>Insert Data To Hbase fail problems is input data error inputData=" + Json);
                } else {
                    int num = re.getNumExceptions();
                    logger.error("HBase Error:"+num,e);
                }
            }
            flag = retry(processData);
        }
        if   (!flag) {
            producer.sendKfkMessage(resourceManager.getHbaseFailTopic(),processData.getStorePk(), JSONObject.toJSONString(processData));
        }
        else {
            encryptData(processData.getNameOfCifId());
            //2.发送双写的topic
            producer.sendKfkMessage(resourceManager.getEsEncryptDataTopic(),processData.getStorePk(),JSONObject.toJSONString(processData.getStorePk()));
            transUtils.saveToEs(processData,resourceManager.es());
            transUtils.redis(resourceManager.redis(),processData,resourceManager.getRedisExpireSeconds());

        }
    }

    public boolean retry(CifData data) {
        try {
             resourceManager.getHbase().close();
             resourceManager.setHbase(null);
             long     s          = System.currentTimeMillis();
             boolean  isok       = transUtils.saveToHBase(resourceManager.hbase(),data);

             return  isok;
        } catch (IOException e) {
            logger.error("==SaveToHbase retry fail",e);
        }
        return false;
    }

    private void encryptData(Map<String,Object> nameOfCifId) {
        if (nameOfCifId != null) {
            for (String fieldName : NeedEncryptFieldNames) {
                if (nameOfCifId.containsKey(fieldName)) {
               if(nameOfCifId.get(fieldName) != null)
               {
                   String value = nameOfCifId.get(fieldName).toString();
                   if (!value.startsWith("xy")) {
                       value = AesEncryptionUtil.encrypt(value);
                       nameOfCifId.put(fieldName,value);
                   }
                 }

                }
            }
            nameOfCifId.put("encrypt",1);
        }
    }
}
