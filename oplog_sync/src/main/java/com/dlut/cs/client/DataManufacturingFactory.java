package com.dlut.cs.client;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.puhui.nbsp.cif.commons.KafkaProducer;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Random;

/**
 * Created by john_liu on 2017/5/9.
 */
@Component
public class DataManufacturingFactory implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(DataManufacturingFactory.class);
    @Autowired
    private RecourceManager recourceManager;
    private String          letters         =  "abcdefghijklmnopqrstuvwxyz";
    private String          numbers         =  "0123456789";
    private String          numberswithout0 =  "123456789";
    private String          gender          =  "FM";
    private int             NameLength      =  3;
    private int             IdLength        =  12;
    private int             PhoneLength     =  11;
    private int             Agelength       =  2;
    @Setter
    @Getter
    private boolean         running         =  true;
    private Thread          thread;

    @PostConstruct
    public void init() {
        thread      = new Thread(this);
    }

    public void start(){
        thread.start();
    }
    public void run() {
        DBCollection datatable = recourceManager.Admin_mongo().getCollection(recourceManager.getMongoServerDataCollection());
        while(running) {
            BasicDBObject doc = new BasicDBObject();
            doc.put("name",RandomName(NameLength));
            doc.put("ID",RandomId(IdLength));
            doc.put("Gender",RandomGender());
            doc.put("Phone",RandomPhone(PhoneLength));
            doc.put("Phone",RandomAge(Agelength));
            System.out.println(datatable.insert(doc));

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("exception when making data",e);
            }

        }

    }
     private String RandomName(int length){
        return  RandomString(letters,length);
    }
    private String RandomId(int length){
         return  RandomString(numbers,length);
    }
    private String RandomGender(){
        return  RandomString(gender,1);
    }
    private String RandomPhone(int length){
        return RandomString(numbers,length);
    }
    private String RandomAge(int length) {return RandomString(numberswithout0,length);}
     private String RandomString(String dic,int length){
         Random random = new Random();
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < length; i++) {
             int number = random.nextInt(dic.length());
             sb.append(dic.charAt(number));
         }
         return sb.toString();
     }
}
