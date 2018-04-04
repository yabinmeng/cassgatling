package cassandra

import scala.concurrent.duration.DurationInt

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.ConsistencyLevel
import com.datastax.driver.core.PlainTextAuthProvider
import com.datastax.driver.core.policies.TokenAwarePolicy
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation

import io.github.gatling.cql.Predef._
import scala.collection.JavaConverters._
import scala.collection.immutable.{HashMap, ListSet}

class MyTestSimu extends Simulation {
  val keyspace = "testks"
  val test_tbl = "tbl1"
  val contactPoints = "10.240.0.4"
  val localDCName = "DC1"

  val cluster = Cluster.builder()
                .addContactPoint(contactPoints)
                .withAuthProvider(new PlainTextAuthProvider("cassandra", "cassandra"))
                .withLoadBalancingPolicy(new TokenAwarePolicy(
                    DCAwareRoundRobinPolicy.builder().withLocalDc(localDCName).build()))
                .build()

  val session = cluster.connect()
  session.execute(s"""CREATE KEYSPACE IF NOT EXISTS $keyspace 
                      WITH replication = { 'class' : 'NetworkTopologyStrategy', 'DC1': '2' }""")

  session.execute(s"USE $keyspace")
  val cqlConfig = cql.session(session)

  // Create Test Table
  session.execute(s"""CREATE TABLE IF NOT EXISTS $test_tbl (
                        cola text PRIMARY KEY,
                        colb text,
                        colc map<text, frozen<list<int>>>,
                        cold set<int>,
                        cole boolean
                      ) WITH compaction = {'class': 'LeveledCompactionStrategy'}
                        AND gc_grace_seconds = 172800;
                   """)


  // Maximum number of (randomly generated) items in a set
  val MAX_MAP_ITEM_NUM = 20

  // Maximum number of (randomly generated) items in a map
  val MAX_SET_ITEM_NUM = 5

  val random = new util.Random

  // generate a (random) alphanumeric string with certain length
  def genRandomString(len : Int) : String = {
    return random.alphanumeric.take(len).mkString
  }

  // generate a set with random number of integers (Scala return type)
  // -------------------------------------------------
  // note: Scala Set type can't be used in CQL prepared statement directly
  //       and needs to be converted to Java type as in genRandomResgrpSet2()
  //       function
  def genRandomIntSet() : Set[Int] = {

    val itemNum : Int = random.nextInt(MAX_SET_ITEM_NUM)
    var myIntSet : Set[Int] = new ListSet[Int]()

    var i = 0
    do  {
      myIntSet += random.nextInt()
      i = i + 1
    } while (i < itemNum)

    return myIntSet
  }

  // generate a set with random number of integers
  def genRandomIntSet2() : java.util.Set[Int] = {
    return genRandomIntSet().asJava
  }

  // generate a map with a random number of items with
  //   "String" as the key type and "Tuple<int, int>" as the value type
  // -------------------------------------------------
  // note: Scala Map type can't be used in CQL prepared statement directly
  //       and needs to be converted to Java type as in genRandomResgrpSet2()
  //       function
  def genRandomStrTup2Map() : Map[String, Tuple2[Int, Int]] = {
    val tval = new Tuple2[Int, Int](random.nextInt(), random.nextInt())
    return Map(genRandomString(22) -> tval)
  }

  // good to be used in CQL prepared statement
  // -------------------------------------------------
  // note: there is no native "Tuple<int, int>" type in Java, use a List with 2 integers
  //       to simulate
  def genRandomStrTup2Map2() : java.util.Map[String, java.util.List[Int]] = {
    val itemNum : Int = random.nextInt(MAX_MAP_ITEM_NUM)
    var myStrTup2Map2 = new HashMap[String, java.util.List[Int]]()

    var i = 0
    do {
      val intList2 = List.fill[Int](2)(random.nextInt()).asJava
      myStrTup2Map2 += (genRandomString(22) -> intList2)

      i = i + 1

    } while (i < itemNum)

    return myStrTup2Map2.asJava
  }

  // Upsert Statement
  val upsertStmt = session.prepare(s"""UPDATE $test_tbl
                                       SET colb=?, colc=colc+?, cold=cold+?, cole=?
                                       WHERE cola=?""")

  // Read Statement
  val readStmt = session.prepare(s"""SELECT * FROM $test_tbl WHERE cola=?""")

  // Random Column value generator 
  val feeder = Iterator.continually(
      // this feader will "feed" random data into our Sessions
      Map(
          "randomCola" -> genRandomString(12),
          "randomColb" -> genRandomString(23),
          "randomColc" -> genRandomStrTup2Map2(),
          "randomCold" -> genRandomIntSet2(),
          "randomCole" -> random.nextBoolean()        
          ))

  // Write with LOCAL_ONE
  val myWriteTestScn = scenario("Write Workload Scenario").repeat(1) {
    feed(feeder)
    .exec(cql("upsertStmt")
        .execute(upsertStmt)
        .withParams("${randomColb}", "${randomColc}", "${randomCold}", "${randomCole}", "${randomCola}")
        .consistencyLevel(ConsistencyLevel.LOCAL_ONE))
  }

  // Read with LOCAL_QUORUM
  val myReadTestScn = scenario("Read Workload Scenario").repeat(1) {
    feed(feeder)
    .exec(cql("readStmt")
        .execute(readStmt)
        .withParams("${randomCola}")
        .consistencyLevel(ConsistencyLevel.LOCAL_QUORUM))
  }

  setUp(
    // Injects random number of users (20~50) per second for 10 minutes 
    myWriteTestScn.inject(
      rampUsersPerSec(20) to 50 during(10 minutes)
    ).protocols(cqlConfig),

    // Injects constant number of users (30) per second for 5 minutes
    myReadTestScn.inject(
      constantUsersPerSec(30) during(5 minutes)
    ).protocols(cqlConfig)
  )

  after(cluster.close())
}
