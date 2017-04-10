package com.dlut.cs.client;

import com.puhui.nbsp.cif.CifException;
import com.puhui.nbsp.cif.CifTransHead;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by john_liu on 2017/3/31.
 */
@Component
public class OplogScannerServer {
    private static final Logger logger = LoggerFactory.getLogger(OplogScannerServer.class);
    @Getter
    private long[]  offsetInfo;
    @Autowired
    @Getter
    private Scanner scanner;
    @Autowired
    public Consumer consumer;

    public void  saveOffset() {scanner.saveOffset();}

    public void  shutdown() {scanner.shutdown();}

    public void start() {
        try {
            scanner.start();
            logger.info("scanner thread start work: "+scanner.getName());
         offsetInfo   = scanner.getOffsetInfo();

        } catch (CifException e) {
            logger.error("",e);
        }
        try{
            consumer.execute();
        } catch (CifException e) {
            logger.error("",e);
        }
    }

}
