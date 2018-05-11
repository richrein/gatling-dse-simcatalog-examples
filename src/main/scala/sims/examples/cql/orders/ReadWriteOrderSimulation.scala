package sims.examples.cql.orders

import actions.examples.cql.OrderActions
import com.datastax.gatling.plugin.CqlPredef._
import com.datastax.gatling.stress.core.BaseSimulation
import com.datastax.gatling.stress.libs.{FetchBaseData, SimConfig}
import feeds.examples.cql.OrderFeed
import io.gatling.core.Predef._

class ReadWriteOrderSimulation extends BaseSimulation {

  val simName = "examples"

  /**
    * Start Write Scenario Setup
    */
  // load conf based on the simName and scenarioName from application.conf for writeOrderPercent
  val simConfWrite = new SimConfig(conf, simName, "readWriteOrder")

  // init orderWriteActions aka queries
  val orderWriteActions = new OrderActions(cass, simConfWrite)

  // Load feed for generating data
  val orderWriteFeed = new OrderFeed().write

  // build scenario to run with feed and write action
  val writeScenario = scenario("OrderWrite")
      .feed(orderWriteFeed)
      .exec(orderWriteActions.writeOrder)

  /**
    * End Write Scenario Setup
    */


  /**
    * Start Read Simulation
    */
  // load conf based on the simName and scenarioName from application.conf for writeOrderPercent
  val simConfRead = new SimConfig(conf, simName, "readWriteOrder")

  // init orderReadActions aka queries
  val orderReadActions = new OrderActions(cass, simConfRead)

  // create base data file using config values
  new FetchBaseData(simConfRead, cass).createBaseDataCsv()

  val feederFile = getDataPath(simConfRead)
  val csvFeeder = csv(getDataPath(simConfRead)).random

  val readScenario = scenario("OrderRead")
      .feed(csvFeeder)
      .exec(orderReadActions.readOrder)
  /**
    * End Read Scenario Setup
    */

  // setup the traffic to run w/ the scenario
  setUp(
    // Both scenarios will be run at the same time asynchronously
    loadGenerator.rampUpToConstant(writeScenario, simConfWrite),
    loadGenerator.rampUpToConstant(readScenario, simConfRead)
  ).protocols(cqlProtocol)

}