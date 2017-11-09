package cn.kengkeng

import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client.Put
import org.apache.hadoop.hbase.util.Bytes
import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

import scala.util.Try

/**
 * Hello world!
 *
 */
object App extends Application {


  override def main(args: Array[String]) {

    val conf = new SparkConf().setMaster("local[2]").setAppName("sparkStreamingTest");
    val ssc = new StreamingContext(conf, Seconds(10));

    val data = ssc.textFileStream("hdfs://localhost:9000/usr/sparkStreamingTest/");
    //  val data = ssc.socketTextStream("localhost", 9999);
    val words = data.flatMap(_.split(","));
    val wordCounts = words.map(x => (x, 1)).reduceByKey((x: Int, y: Int) => x + y);
    wordCounts.print();

    //将生成的wordCount结果存到hbase中
    wordCounts.foreachRDD(rdd => {

      rdd.foreachPartition(partition =>{

        val connection = HbaseUtil.getHbaseConn;
        partition.foreach(data =>{

          val tableName = TableName.valueOf("wordCount");
          val table = connection.getTable(tableName);

          try {

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

          }catch {

            case e : Exception =>

              e.printStackTrace();

          }finally {

            table.close();

          }

        })

      })

    })

    ssc.start()
    ssc.awaitTermination()

  }
}
