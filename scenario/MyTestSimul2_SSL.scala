package cassandra

import java.io.{FileInputStream, InputStream}
import java.security.KeyStore
import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID

import scala.concurrent.duration.DurationInt
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.policies.{DCAwareRoundRobinPolicy, TokenAwarePolicy}
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.github.gatling.cql.Predef._
import javax.net.ssl.{SSLContext, TrustManagerFactory}
import java.util.Date

import scala.collection.JavaConverters._
import scala.collection.immutable.{HashMap, ListSet}

class MyTestSimu extends Simulation {
  val keyspace = "testks"
  val test_tbl = "testks.ptnt_cndtn_by_ptnt_cd"
  val contactPoints = "x.x.x.x"
  val localDCName = "DC1"


  val ks = KeyStore.getInstance("JKS")
  val tst = new FileInputStream("/home/automaton/mytruststore")
  ks.load(tst, "cassandra".toCharArray())
  val  tmf = TrustManagerFactory.getInstance("SunX509")
  //val  tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
  tmf.init(ks)


  val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(null, tmf.getTrustManagers(), null)

  val sslOptions = RemoteEndpointAwareJdkSSLOptions.builder()
    .withSSLContext(sslContext)
    .build();


  val cluster = Cluster.builder()
    .addContactPoint(contactPoints)
    .withAuthProvider(new PlainTextAuthProvider("cassandra", "cassandra"))
    .withLoadBalancingPolicy(new TokenAwarePolicy(
      DCAwareRoundRobinPolicy.builder().withLocalDc(localDCName).build()))
    .withSSL(sslOptions)
    .build()


  val session = cluster.connect()
  session.execute(s"""CREATE KEYSPACE IF NOT EXISTS $keyspace
                      WITH replication = { 'class' : 'NetworkTopologyStrategy', 'DC1': '2' }""")


  session.execute(s"""USE $keyspace""")
  val cqlConfig = cql.session(session)

  // Create Test Table
  session.execute(s""" CREATE TABLE IF NOT EXISTS $test_tbl (
                      patient_code uuid,
                      condition_type text,
                      condition_code text,
                      additional_information map<text, text>,
                      from_date timestamp,
                      last_update timestamp,
                      to_date timestamp,
                      PRIMARY KEY (patient_code, condition_type, condition_code)
                    );
                  """)


  // Maximum number of (randomly generated) key/value pairs in a map
  val MAX_MAP_ITEM_NUM = 5

  val random = new util.Random

  // generate a (random) alphanumeric string with certain length
  def genRandomString(len : Int) : String = {
    return random.alphanumeric.take(len).mkString
  }

  // generate a (random) positive integer that is max to a specified
  // positive integer.
  def genRandomInt(ceiling: Int) :  Int = {
    return random.nextInt(ceiling + 1)
  }

  def genRandomUUID() : UUID = {
    return UUID.randomUUID();
  }

  // generate a map with a random number of items with
  //   "String" as the key type and "String" as the value type
  // -------------------------------------------------
  // note: Scala Map type can't be used in CQL prepared statement directly
  //       and needs to be converted to Java type as in genRandomStr2StrMap2()
  //       function
  def genRandomStr2StrMap() : Map[String, String] = {

    val itemNum : Int = random.nextInt(MAX_MAP_ITEM_NUM)
    var myStr2StrMap = new HashMap[String, String]()

    var i = 0
    do  {
      myStr2StrMap += (genRandomString(genRandomInt(10)) -> genRandomString(genRandomInt(50)))
      i = i + 1
    } while (i < itemNum)

    return myStr2StrMap
  }

  // good to be used in CQL prepared statement
  // -------------------------------------------------
  // note: there is no native "Tuple<int, int>" type in Java, use a List with 2 integers
  //       to simulate
  def genRandomStr2StrMap2() : java.util.Map[String, String] = {
    return genRandomStr2StrMap().asJava
  }

  def getToday() : Date = {
    val today = java.time.LocalDate.now()
    val zdt = today.atStartOfDay(ZoneId.systemDefault())
    return Date.from(zdt.toInstant)
  }

  // generate a date as a random number of days after today
  def genRandomDate(maxDays: Int) : Date = {
    val days = genRandomInt(maxDays)
    val today = java.time.LocalDate.now()
    val rndmDate = today.plusDays(days)

    val zdt = rndmDate.atStartOfDay(ZoneId.systemDefault())

    return Date.from(zdt.toInstant)
  }


  // Random Column value generator
  val feeder = Iterator.continually(
    // this feader will "feed" random data into our Sessions
    Map(
      "patient_code" -> genRandomUUID(),
      "condition_type" -> genRandomString(10),
      "condition_code" -> genRandomString(5),
      "additional_information" -> genRandomStr2StrMap2(),
      "from_date" -> getToday(),
      "last_update" -> getToday(),
      "to_date" -> genRandomDate(genRandomInt(10))
    )
  )


  // Upsert Statement
  val upsertStmt = session.prepare(s"""UPDATE $test_tbl
                                     SET additional_information=additional_information+?, from_date=?, last_update=?, to_date=?
                                     WHERE patient_code=? and condition_type=? and condition_code=?""")

  // Read Statement
  val readStmt = session.prepare(s"""SELECT * FROM $test_tbl
                                      WHERE patient_code=? and condition_type=? and condition_code=?""")

  // Write with LOCAL_QUORUM
  val myWriteTestScn = scenario("Write Workload Scenario").repeat(1) {
    feed(feeder)
      .exec(cql("Write_Simulation")
        .execute(upsertStmt)
        .withParams(
          "${additional_information}",
          "${from_date}",
          "${last_update}",
          "${to_date}",
          "${patient_code}",
          "${condition_type}",
          "${condition_code}")
        .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM))
  }

  // Read with LOCAL_QUORUM
  val myReadTestScn = scenario("Read Workload Scenario").repeat(1) {
    feed(feeder)
      .exec(cql("Read_Simulation")
        .execute(readStmt)
        .withParams(
          "${patient_code}",
          "${condition_type}",
          "${condition_code}")
        .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM))
  }

  setUp(
    // Constan injestion rate: # of users per sec., for 30 mins
    // write/read ratio is 9:1

    myWriteTestScn.inject(
      //rampUsersPerSec(20) to 50 during(10 minutes)
      constantUsersPerSec(9000) during(30 minutes)
    ).protocols(cqlConfig),

    myReadTestScn.inject(
      constantUsersPerSec(1000) during(30 minutes)
    ).protocols(cqlConfig)
  )

  after(cluster.close())
}
