## Description
This is a sample code of utilizing [Gatling](https://gatling.io/) testing framework for stress testing with DSE

### Pre-requisites
Note: At the moment, the latest version of Gatling framework is 2.3.1. However, the CQL plugin 0.0.7 is not compatible with Gatling 3.1.5 yet. 

1. Gatling high charts bundle version 2.2.5: 
  + https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/   
2. Gatling CQL plugin v0.0.7 (latest version as of writing)
  + https://github.com/gatling-cql/GatlingCql/releases

### Procedure
1. Set up Gatling framework and the CQL plug-in (just unzip, as per description found [here](https://github.com/gatling-cql/GatlingCql))
2. Download the MyTestSimu.scala file and put it under folder <GATLING_HOME>/user-files/simulations/
3. Execute the Gatling simulation (stress-testing) scenario by running command: <GATLING_HOME>/bin/gatling.sh. Follow the instructions on the command-line output. 

---

An example is as below. Simulation number 1 is the Cassandra stres-testing scenario as defined by this example.

```
$ bin/gatling.sh
GATLING_HOME is set to /home/automaton/gatling-charts-highcharts-bundle-2.2.5
Choose a simulation number:
     [0] cassandra.CassandraSimulation
     [1] cassandra.MyTestSimu
     [2] computerdatabase.BasicSimulation
     [3] computerdatabase.advanced.AdvancedSimulationStep01
     [4] computerdatabase.advanced.AdvancedSimulationStep02
     [5] computerdatabase.advanced.AdvancedSimulationStep03
     [6] computerdatabase.advanced.AdvancedSimulationStep04
     [7] computerdatabase.advanced.AdvancedSimulationStep05
1
Select simulation id (default is 'mytestsimu'). Accepted characters are a-z, A-Z, 0-9, - and _

Select run description (optional)

Simulation cassandra.MyTestSimu started...

================================================================================
2018-03-30 01:38:13                                           5s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=92     KO=0     )
> upsertStmt                                               (OK=92     KO=0     )

---- My Test Table Load Scenario -----------------------------------------------
[                                                                          ]  0%
          waiting: 25108  / active: 0      / done:92
================================================================================
... ...

Simulation cassandra.MyTestSimu completed in 720 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                      25200 (OK=25200  KO=0     )
> min response time                                      0 (OK=0      KO=-     )
> max response time                                    253 (OK=253    KO=-     )
> mean response time                                     7 (OK=7      KO=-     )
> std deviation                                          7 (OK=7      KO=-     )
> response time 50th percentile                          7 (OK=7      KO=-     )
> response time 75th percentile                         11 (OK=11     KO=-     )
> response time 95th percentile                         12 (OK=12     KO=-     )
> response time 99th percentile                         22 (OK=22     KO=-     )
> mean requests/sec                                     35 (OK=35     KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                         25200 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================

Reports generated in 5s.
Please open the following file: /home/automaton/gatling-charts-highcharts-bundle-2.2.5/results/mytestsimu-1522365239303/index.html
```
