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
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

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
    private int             AgeLength       =  2;
    private int             ScoreLength     =  2;
    private Set             idSet           =  new HashSet();
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
            //BasicDBObject doc_accounts_info = new BasicDBObject();
            String        id  =     RandomId(IdLength);
            if(idSet.add(id))
            {
                continue;
            }
            for(int i = 1 ;i <7;i++){

                this.WriteIntoDatabase(doc,id,Integer.toString(i));

            }

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
    private String RandomScore(int length){return RandomString(numberswithout0,length);}
     private String RandomString(String dic,int length){
         Random random = new Random();
         StringBuffer sb = new StringBuffer();
         for (int i = 0; i < length; i++) {
             int number = random.nextInt(dic.length());
             sb.append(dic.charAt(number));
         }
         return sb.toString();
     }
    private void WriteIntoDatabase(BasicDBObject doc,String id,String InsertTime){
        doc.put("name",RandomName(NameLength));
        doc.put("ID",id);
        doc.put("Gender",RandomGender());
        doc.put("Phone",RandomPhone(PhoneLength));
        doc.put("Age",RandomAge(AgeLength));
        doc.put("Taobao",RandomScore(ScoreLength));
        doc.put("Mobile",RandomScore(ScoreLength));
        doc.put("CreditCard",RandomScore(ScoreLength));
        doc.put("ThirdPartyCredit",RandomScore(ScoreLength));
        doc.put("datatime",InsertTime);
    }
}
