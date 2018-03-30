This is a sample code of utilizing [Gatling](https://gatling.io/) testing framework for stress testing with DSE

## Pre-requisites

Note: At the moment, the latest version of Gatling framework is 2.3.1. However, the CQL plugin 0.0.7 is not compatible with Gatling 3.1.5 yet. 

1. Gatling high charts bundle version 2.2.5: 
  + https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/   
2. Gatling CQL plugin v0.0.7 (latest version as of writing)
  + https://github.com/gatling-cql/GatlingCql/releases

## Procedure

1. Set up Gatling framework and the CQL plug-in (just unzip, as per description found [here](https://github.com/gatling-cql/GatlingCql))
2. Download the MyTestSimu.scala file and put it under folder <GATLING_HOME>/user-files/simulations/
3. Execute the Gatling simulation (stress-testing) scenario by running command: <GATLING_HOME>/bin/gatling.sh
