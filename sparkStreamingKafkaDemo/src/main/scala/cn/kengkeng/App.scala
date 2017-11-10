package cn.kengkeng

import java.sql.{PreparedStatement, Statement}
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import scala.util.Try

/**
 * author by kengkeng
 *
 */
object App extends Application {

  override def main(args: Array[String]) {

    val conf = new SparkConf().setMaster("local[2]").setAppName("sparkStreamingTest");
    val ssc = new StreamingContext(conf, Seconds(10));

//    val data = ssc.textFileStream("hdfs://localhost:9000/usr/sparkStreamingTest/");
    //  val data = ssc.socketTextStream("localhost", 9999);

    //从kafka中读取数据，并用于单词计数
    val kafkaConsumerGroupId = "wordCountConsumer"
    val zkServers = "localhost:2181"
    val kafkaStreams = KafkaUtils.createStream(ssc, zkServers, kafkaConsumerGroupId, Map("wordCounts" -> 1))

    val data = kafkaStreams.map(_._2)

    val words = data.flatMap(_.split(","));
    val wordCounts = words.map(x => (x, 1)).reduceByKey((x: Int, y: Int) => x + y);
    wordCounts.print();

    //将生成的wordCount结果存到mysql,hbase和redis中
    wordCounts.foreachRDD(rdd => {

      rdd.foreachPartition(partition =>{

        //获取hbase连接对象
        val connection = HbaseUtil.getHbaseConn;

        //获取redis连接对象
        val jedis = RedisClient.pool.getResource

        //获取mysql连接对象
        val conn = MysqlUtil.getConnection();
        var stmt : PreparedStatement = null;
        val statement : Statement = conn.createStatement();

        try{

          partition.foreach(data =>{

            val tableName = TableName.valueOf("wordCount");
            val table = connection.getTable(tableName);

            try {

              //存储到hbase中
              var rowKey = data._1 + data._2;
              if(rowKey.length() < 15) {

                var i =0;
                for(i <- (1 to 15-rowKey.length())){

                  rowKey = "$" + rowKey;

                }

              }
              val put = new Put(Bytes.toBytes(rowKey));
              put.addColumn("data".getBytes(), "word".getBytes(), data._1.getBytes());
              put.addColumn("data".getBytes, "count".getBytes, data._2.toString.getBytes);
              Try(table.put(put)).getOrElse(table.close());

              //存储到redis中
              //words集合，存储所有的word
              jedis.sadd("words", data._1);
              //wordCounts散列，用于存储word和对应的数量，key为word，value为word对应的值
              jedis.hset("wordCounts", data._1, data._2.toString);

              //存储到mysql中
              //测试批量插入更新操作
              statement.addBatch("insert into wordCount(word, count) values ('" + data._1 + "','" + data._2 +
                "') on duplicate key update count = '" + data._2 + "'");
//              //测试存在则更新，不存在则插入操作
//              val update_sql = "insert into wordCount(word, count) values (?, ?) on duplicate key update count = ?";
//              stmt = conn.prepareStatement(update_sql);
//              stmt.setString(1, data._1);
//              stmt.setInt(2, data._2);
//              stmt.setInt(3, data._2);
//              stmt.executeUpdate();

            }catch {

              case e : Exception =>

                e.printStackTrace();

            }finally {

              table.close();

            }

          })

          statement.executeBatch();

        }catch {

          case e : Exception =>e.printStackTrace();

        }finally{

          if(stmt != null){

            stmt.close();

          }

          if(conn != null){

            conn.close();

          }

        }

        RedisClient.pool.returnResource(jedis)

      })

    })

    ssc.start()
    ssc.awaitTermination()

  }

}
