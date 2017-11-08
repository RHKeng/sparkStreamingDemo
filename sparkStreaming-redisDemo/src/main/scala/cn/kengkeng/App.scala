package cn.kengkeng

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

/**
 * Hello world!
 *
 */
object App extends Application {

  override def main(args: Array[String]){

    val conf = new SparkConf().setMaster("local[2]").setAppName("sparkStreamingTest");
    val ssc = new StreamingContext(conf, Seconds(10));

    val data = ssc.textFileStream("hdfs://localhost:9000/usr/sparkStreamingTest/");
    //  val data = ssc.socketTextStream("localhost", 9999);
    val words = data.flatMap(_.split(","));
    val wordCounts = words.map(x => (x, 1)).reduceByKey((x: Int, y: Int) => x+y);
    wordCounts.print();

    //存储数据到redis中
    wordCounts.foreachRDD(rdd => {

      rdd.foreachPartition(partition => {

        //获取redis连接对象
        val jedis = RedisClient.pool.getResource
        partition.foreach(wordCount => {

          //words集合，存储所有的word
          jedis.sadd("words", wordCount._1);
          //wordCounts散列，用于存储word和对应的数量，key为word，value为word对应的值
          jedis.hset("wordCounts", wordCount._1, wordCount._2.toString);

        })
        RedisClient.pool.returnResource(jedis)

      })

    })

    ssc.start()
    ssc.awaitTermination()

  }

}
