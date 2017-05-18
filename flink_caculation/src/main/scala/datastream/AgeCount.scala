package datastream

import java.io.FileInputStream
import java.net.InetSocketAddress
import java.util.Properties
import org.apache.flink.streaming.api.scala._

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.api.java.typeutils.TypeExtractor
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.elasticsearch2.{ElasticsearchSink, ElasticsearchSinkFunction, RequestIndexer}
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer08
import org.apache.flink.streaming.util.serialization.SimpleStringSchema
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.Requests
import utils.KafkaStringSchema

import scala.util.parsing.json.{JSON, JSONObject}

/**
  * Created by john_liu on 2017/5/15.
  */
object WindowAgeCount {
  
  private val ZOOKEEPER_HOST = "localhost:2181"
  private val KAFKA_BROKER = "localhost:9092"
  private val KAFKA_GROUP = "result_set"

  case class AgeData( age:String, count:Int)
  def main(args: Array[String]): Unit = {

    //flink 环境
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime)
    env.enableCheckpointing(1000)
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
//    //kafka配置文件读取
//    val prop = new Properties()
//    val path = "/Users/john_liu/graduation_design/oplog_sync/src/main/resources/META-INF/app.properties"
//    //val path = ""
//    prop.load(new FileInputStream(path))
    //kafka config
    val props = new Properties
    props.setProperty("zookeeper.connect", ZOOKEEPER_HOST)
    props.setProperty("bootstrap.servers", KAFKA_BROKER)
    props.setProperty("group.id", KAFKA_GROUP)
    //建立kafka consumer链接
    val kafkaConsumer = new FlinkKafkaConsumer08[String](
      "grau_info",KafkaStringSchema, props)
    //生成数据流
    val datastream = env.addSource(kafkaConsumer)
    //datastream.print()
    //mapreduce
    val  resultStream  = datastream.map{x =>val kk =AgeData(division(dataExtraction(x)("Age").asInstanceOf[String]),1);kk}.keyBy("age").timeWindow(Time.seconds(5)).sum("count")
    resultStream.print()



    //ElasticSearch配置
    val config: java.util.Map[String,String] = new java.util.HashMap[String, String]
    config.put("bulk.flush.max.actions", "1")
    config.put("cluster.name", "elasticsearch")

    val transports = new java.util.ArrayList[InetSocketAddress]()
    transports.add(new InetSocketAddress("localhost", 9300))

    val elasticsearchSink = new ElasticsearchSinkFunction[AgeData]{
      def createIndexRequest(element: AgeData): IndexRequest = {
        val map = new java.util.HashMap[String, String]
        map.put("age",element.age)
        map.put("quan",element.count.toString)
        map.put("timestamp")
        Requests.indexRequest.index("grau").`type`("window_age_count_result").source(map)
      }


      override def process(element: AgeData, runtimeContext: RuntimeContext, requestIndexer: RequestIndexer): Unit = requestIndexer.add(createIndexRequest(element))
    }
    val essink  = new ElasticsearchSink(config,transports,elasticsearchSink) with SinkFunction[(AgeData]{}

    resultStream.addSink()
    env.execute("Flink_AgeCount_ElasticSearch2")
  }
 def dataExtraction(source :String)(name:String) = {

   val Option = JSON.parseFull(source.split("\t")(1))

   val kk = Option.get.asInstanceOf[ Map[String, Any]].get(name)
   Option match {
     // Matches if jsonStr is valid JSON and represents a Map of Strings to Any
     case Some(map: Map[String, Any])=> val kk = Option.get.asInstanceOf[ Map[String, Any]].get(name).get;kk.asInstanceOf[String]
     case None => println("Parsing failed");
     case other => println("Unknown data structure: " + other)
   }


 }

  def division(x:String) ={
   (x.toInt)/10.toInt match {
     case 0 => "0"
     case 1 => "1"
     case 2 => "2"
     case 3 => "3"
     case 4 => "4"
     case 5 => "5"
     case 6 => "6"
     case 7 => "7"
     case 8 => "8"
     case 9 => "9"
     case int:Int =>"10"
     case any => throw new Exception
   }
 }
}
