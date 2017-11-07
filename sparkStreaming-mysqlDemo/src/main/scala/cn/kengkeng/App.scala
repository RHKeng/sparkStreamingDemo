package cn.kengkeng

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import StreamingContext._
import org.apache.spark.streaming.Seconds
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, Statement}

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

    //保存统计数据到mysql对应的表中
    wordCounts.foreachRDD(rdd => {

      //针对每个partition进行保存操作，每个partition创建一个连接mysql对象，并用连接池维护
//      rdd.foreachPartition(partitions=>{
//
//        val conn = ConnectPoolUtil.getConnection();
//        var stmt : PreparedStatement = null;
//        partitions.foreach(word =>{
//          val sql = "insert into wordCount(word, count) values (?, ?)";
//          stmt = conn.prepareStatement(sql);
//          stmt.setString(1, word._1);
//          stmt.setInt(2, word._2);
//          stmt.executeUpdate();
//        })
//
//      }
//      )


      //使用函数式编程方法，具体实现内容与上面相同
      def myFun(iterator: Iterator[(String, Int)]) {

        val conn = ConnectPoolUtil.getConnection();
        var stmt : PreparedStatement = null;
        var statement : Statement = null;

        try {

          statement = conn.createStatement();

          iterator.foreach(word =>{

//            //先执行删除操作
//            val delete_sql = "delete from wordCount where word = ?";
//            stmt = conn.prepareStatement(delete_sql);
//            stmt.setString(1, word._1);
//            stmt.executeUpdate();
//
//            val sql = "insert into wordCount(word, count) values (?, ?)";
//            stmt = conn.prepareStatement(sql);
//            stmt.setString(1, word._1);
//            stmt.setInt(2, word._2);
//            stmt.executeUpdate();

//            //测试存在则更新，不存在则插入操作
//            val update_sql = "insert into wordCount(word, count) values (?, ?) on duplicate key update count = ?";
//            stmt = conn.prepareStatement(update_sql);
//            stmt.setString(1, word._1);
//            stmt.setInt(2, word._2);
//            stmt.setInt(3, word._2);
//            stmt.executeUpdate();

            //测试批量插入更新操作
            statement.addBatch("insert into wordCount(word, count) values ('" + word._1 + "','" + word._2 +
            "') on duplicate key update count = '" + word._2 + "'");

          })

          statement.executeBatch();

        }catch {

          case e : Exception =>e.printStackTrace();

        }finally {

          if(stmt != null){

            stmt.close();

          }

          if(conn != null){

            conn.close();

          }

        }

      }

      val repartitionedRDD = rdd.repartition(3)
      repartitionedRDD.foreachPartition(myFun);

    })

    ssc.start()
    ssc.awaitTermination()

  }


}
