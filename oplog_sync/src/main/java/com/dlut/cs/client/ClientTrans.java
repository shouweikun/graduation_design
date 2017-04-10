package com.dlut.cs.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.DBObject;
import com.puhui.nbsp.cif.CifException;
import com.puhui.nbsp.cif.CifTransHead;
import com.puhui.nbsp.cif.commons.JedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by john_liu on 2017/3/28.
 */
public class ClientTrans {
    private static final Logger logger  = LoggerFactory.getLogger(JedisUtil.class);
    private static final DateFormat dfs = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    @Autowired
    private              RecourceManager recourceManager;

    public ClientTrans() {
    }

    public CifTransHead transBean(String rawJson,long timestamp) {

        JSONObject  rawObj              = null;
        try {
            rawObj                      = JSON.parseObject(rawJson);
        } catch (JSONException e) {
            throw new CifException("transJson parse json errpor",e);
        }
        String      ns                  = rawObj.getString("ns");
        String      ts                  = rawObj.getString("ts");
        ts                              = JSON.parseObject(ts).getString("time");
        String[]    nss                 = ns.split("\\.");
        String      hash                = rawObj.getString("h");
        String      op                  = rawObj.getString("op");
        String      opStr               = "";
        switch (op.trim().charAt(0)) {
            case 'i':
                opStr                   = CifTransHead.EventType.INSERT.name();
                break;
            case 'u':
                opStr                   = CifTransHead.EventType.UPDATE.name();
                break;
            case 'd':
                opStr                   = CifTransHead.EventType.DELETE.name();
                break;
            default:
                break;
        }

        CifTransHead    head            = new CifTransHead();
        head.setTsTime(ts);
        head.setBiz("channel-oplog");
        if (nss.length != 2) {
            logger.error("==>dbname and TableName connot be empty");
            return null;
        }
        else {
            head.setDbName(nss[0]);
            head.setTableName(nss[1]);

        }
        head.setEventType(opStr);
        head.setLogName(hash);
        Date           now             = new Date();
        head.setOplogReceiveLogDateWithDate(now);
        head.setOplogReceiveLogDateWithDateStr(dfs.format(now));

        String         JsonBody        = "";
        if ("UPDATE".equals(opStr)) {
        String         o               = rawObj.getString(" ");
                //填写对应的库字段，用于更新
        JSONObject    object           = JSONObject.parseObject(o);
        String        primarykey       = object.getString("_id");
        try {
        DBObject      dbObject         = recourceManager.mdc().use(head.getDbName()).getCollection(head.getTableName()).findOne(primarykey);
        JsonBody                       = JSONObject.toJSONString(dbObject);
        } catch (RuntimeException e) {
        String        msg              = String.format("find DBobject from MongoDb :%s.%s",head.getDbName(),head.getTableName(),primarykey);
        throw new CifException(msg,primarykey,e);
        }
        }else {
        JsonBody                       = rawObj.getString("o");
        }
        head.setJsonBody(JsonBody);
        return      head;
    }
}
