package datastream

import java.io.FileInputStream
import java.net.InetSocketAddress
import java.util.Properties

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment, _}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.elasticsearch2.{ElasticsearchSink, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests
import utils.KafkaStringSchema

/**
  * Created by john_liu on 2017/5/10.
  */
object GenderCount {
  def main(args: Array[String]): Unit = {
    //flink 环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    //kafka配置文件读取
    val prop = new Properties()
    val path = "/Users/john_liu/graduation_design/oplog_sync/src/main/resources/META-INF/app.properties"
    //val path = ""
    prop.load(new FileInputStream(path))

    //建立kafka consumer链接
    val kafkaConsumer = new FlinkKafkaConsumer08[String](
      "grau_info", KafkaStringSchema, prop)
    val datastream = env.addSource(kafkaConsumer)
     val resultStream = datastream.map{(_,1)}.keyBy(0).timeWindow(Time.seconds(500)).sum(1).map(x=>(x._1,x._2.toString))



   //ElasticSearch配置
   val config: java.util.Map[String,String] = new java.util.HashMap[String, String]
    config.put("bulk.flush.max.actions", "1")
    config.put("cluster.name", "elasticsearch")

    val transports = new java.util.ArrayList[InetSocketAddress]()
    transports.add(new InetSocketAddress("localhost", 9300))

    val elasticsearchSink = new ElasticsearchSinkFunction[(String,String)]{
      def createIndexRequest(element: (String,String)): IndexRequest = {
        val map = new java.util.HashMap[String, String]
        if(element._1.contains(",")){
          val k = element._1.split(",")
          map.put("log1",k(1))
          map.put("data1", k(0))
        }
        Requests.indexRequest.index("grau").`type`("window_age_count_result").source(map)
      }
      def process(element: (String,String), runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = requestIndexer.add(createIndexRequest(element))
    }
    val essink  = new ElasticsearchSink(config,transports,elasticsearchSink) with SinkFunction[(String,String)]{}

    resultStream.addSink(essink)
    }

  def mark(x:String) ={
    if(x.trim.equals("F")) ("0")
    else ("1")
  }

}
