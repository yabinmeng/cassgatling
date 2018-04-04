## Description
This is a sample code of utilizing [Gatling](https://gatling.io/) testing framework for stress testing with DSE

### Pre-requisites
Note: At the moment, the latest version of Gatling framework is 2.3.1. However, the CQL plugin 0.0.7 is not compatible with Gatling 3.1.5 yet. 

1. Gatling high charts bundle version 2.2.5: 
  + https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/2.2.5/   
2. Gatling CQL plugin v0.0.7 (latest version as of writing)
  + https://github.com/gatling-cql/GatlingCql/releases
 
### Notes about Example Simulation Scenario
The simulation scenario (MyTestSimu.scala) as included in this example simulates a mixed read/write workload. The core steps of the scenario are summarized below and you can follow the same steps when creating your own scenario:
+. Set up connection to Cassandra/DSE cluster with proper properties, such as:
** contact points
** username/password (if authentication is enabled)
** load balancing policy
** etc.

### Procedure
1. Set up Gatling framework and the CQL plug-in (just unzip, as per description found [here](https://github.com/gatling-cql/GatlingCql))
2. Download the MyTestSimu.scala file and put it under folder <GATLING_HOME>/user-files/simulations/
3. Execute the Gatling simulation (stress-testing) scenario by running command: <GATLING_HOME>/bin/gatling.sh. Follow the instructions on the command-line output. 

NOTE: The simulation scenario (MyTestSimu.scala) is tested against a DSE cluster (version 5.1.6) with UserName/Password authentication. Please adjust accordingly for your case.

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
2018-04-04 15:33:43                                           5s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=232    KO=0     )
> upsertStmt                                               (OK=93     KO=0     )
> readStmt                                                 (OK=139    KO=0     )

---- Read Workload Scenario ----------------------------------------------------
[#                                                                         ]  1%
          waiting: 8861   / active: 0      / done:139
---- Write Workload Scenario ---------------------------------------------------
[                                                                          ]  0%
          waiting: 20907  / active: 0      / done:93
================================================================================
... ...

================================================================================
2018-04-04 15:43:39                                         600s elapsed
---- Requests ------------------------------------------------------------------
> Global                                                   (OK=30000  KO=0     )
> upsertStmt                                               (OK=21000  KO=0     )
> readStmt                                                 (OK=9000   KO=0     )

---- Read Workload Scenario ----------------------------------------------------
[##########################################################################]100%
          waiting: 0      / active: 0      / done:9000
---- Write Workload Scenario ---------------------------------------------------
[##########################################################################]100%
          waiting: 0      / active: 0      / done:21000
================================================================================

Simulation cassandra.MyTestSimu completed in 600 seconds
Parsing log file(s)...
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                      30000 (OK=30000  KO=0     )
> min response time                                      0 (OK=0      KO=-     )
> max response time                                    224 (OK=224    KO=-     )
> mean response time                                     2 (OK=2      KO=-     )
> std deviation                                          5 (OK=5      KO=-     )
> response time 50th percentile                          1 (OK=1      KO=-     )
> response time 75th percentile                          2 (OK=2      KO=-     )
> response time 95th percentile                          3 (OK=3      KO=-     )
> response time 99th percentile                          7 (OK=7      KO=-     )
> mean requests/sec                                     50 (OK=50     KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                         30000 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================

Reports generated in 4s.
Please open the following file: /home/automaton/gatling-charts-highcharts-bundle-2.2.5/results/mytestsimu-1522856018600/index.html
```
