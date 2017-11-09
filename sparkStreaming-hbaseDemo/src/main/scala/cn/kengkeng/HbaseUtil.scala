package cn.kengkeng

import org.apache.hadoop.hbase.{HBaseConfiguration, HConstants}
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}


/**
  * Created by rhkeng on 17-11-9.
  */
object HbaseUtil extends Serializable{

  //配置生成hbase连接对象
  private val conf = HBaseConfiguration.create();
  private val hbasePort = "2181";
  private val hbaseQuorum = "localhost";
//  conf.set("hbase.zookeeper.property.clientPort", hbasePort);
//  conf.set("hbase.zookeeper.quorum", hbaseQuorum);
  conf.set(HConstants.ZOOKEEPER_CLIENT_PORT, hbasePort);
  conf.set(HConstants.ZOOKEEPER_QUORUM, hbaseQuorum);

  private val connection = ConnectionFactory.createConnection(conf);

  def getHbaseConn: Connection = connection;

}
