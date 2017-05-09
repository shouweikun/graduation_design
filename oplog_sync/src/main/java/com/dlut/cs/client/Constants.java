package com.dlut.cs.client;

/**
 * Created by john_liu on 2017/3/20.
 */
public class Constants {
    private Constants(){}
    public static  final String OPLOG_NS_PATTERN = "[\\w^0-9]+[\\w]+\\.[\\w^0-9]+[\\w]";
    public static final String OPLOG_OP_PATTERN = "[iud]{1}";
}
