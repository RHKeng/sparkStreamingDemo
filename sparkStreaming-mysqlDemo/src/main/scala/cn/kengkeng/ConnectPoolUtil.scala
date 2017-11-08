package cn.kengkeng

import java.sql.{Connection, PreparedStatement, ResultSet, DriverManager}

/**
  * Created by rhkeng on 17-11-4.
  */
object ConnectPoolUtil extends java.io.Serializable{

  def getConnection(): Connection = {

    var conn : Connection = null;
    val url = "jdbc:mysql://localhost:3306/sparkStreamingTest";
    val user = "root";
    val password = "woaini159357RHK";
    conn = DriverManager.getConnection(url, user, password);
    return conn;

  }

}