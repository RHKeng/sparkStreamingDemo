package cn.kengkeng

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.JedisPool

/**
  * Created by rhkeng on 17-11-8.
  */
object RedisClient extends Serializable{

  val redisHost = "127.0.0.1";
  val redisPort = 6379;
  val redisTimeout = 30000;

  //scala利用lazy使对象可序列化
  lazy val pool = new JedisPool(new GenericObjectPoolConfig(), redisHost, redisPort, redisTimeout);

  lazy val hook = new Thread {

    override def run = {

      println("Execute hook thread: " + this)
      pool.destroy()

    }

    }

  sys.addShutdownHook(hook.run)

  }
