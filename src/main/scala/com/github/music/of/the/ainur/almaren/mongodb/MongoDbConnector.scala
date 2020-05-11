package com.github.music.of.the.ainur.almaren.mongodb

import org.apache.spark.sql.{DataFrame,SaveMode}
import com.github.music.of.the.ainur.almaren.Tree
import com.github.music.of.the.ainur.almaren.builder.Core
import com.github.music.of.the.ainur.almaren.state.core.{Target,Source}
import com.mongodb.spark._
import com.mongodb.spark.config._
import com.mongodb.spark.sql.SparkSessionFunctions

private[almaren] case class SourceMongoDb(
  hosts: String,
  database: String, 
  collection: String,
  user:Option[String],
  password:Option[String],
  options:Map[String,String]) extends Source {

  def source(df: DataFrame): DataFrame = {
    logger.info(s"hosts:{$hosts}, database:{$database}, collection:{$collection}, user:{$user}, options:{$options}")
    SparkSessionFunctions(df.sparkSession).loadFromMongoDB(ReadConfig(
     (user,password) match {
       case (Some(u),Some(p)) => Map("uri" -> s"mongodb://$u:$p@$hosts/$database.$collection") ++ options
       case (_,_) => Map("uri" -> s"mongodb://$hosts/$database.$collection") ++ options
     })
    )}
}


private[almaren] case class TargetMongoDb(
  hosts: String,
  database: String, 
  collection: String,
  user:Option[String],
  password:Option[String],
  options:Map[String,String],
  saveMode:SaveMode) extends Target {

  def target(df: DataFrame): DataFrame = {
    logger.info(s"hosts:{$hosts}, database:{$database}, collection:{$collection}, user:{$user}, options:{$options}, saveMode:{$saveMode}")
    df.write.format("mongo")
      .options(options)
      .mode(saveMode)
      .save
    df
  }

}

private[almaren] trait MongoDbConnector extends Core {
  def targetMongoDb(hosts: String,database: String,collection: String,user:Option[String] = None,password:Option[String] = None,options:Map[String,String] = Map(),saveMode:SaveMode = SaveMode.ErrorIfExists): Option[Tree] =
     TargetMongoDb(hosts,database,collection,user,password,options,saveMode)

  def sourceMongoDb(hosts: String,database: String,collection: String,user:Option[String] = None,password:Option[String] = None,options:Map[String,String] = Map()): Option[Tree] =
    SourceMongoDb(hosts,database,collection,user,password,options)
}

object MongoDb {
  implicit class MongoImplicit(val container: Option[Tree]) extends MongoDbConnector
}
