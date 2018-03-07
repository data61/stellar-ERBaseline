Stellar-ERBaseline
====================

*Original source code sourced from SERF framework http://infolab.stanford.edu/serf/*

## Usage: Docker
https://hub.docker.com/r/data61/stellar-erbaseline


## Quickstart

1.To re-compile this package:

```bash
mvn package
```
2.You can check the `help` command to understand parameters

```bash
java -jar "target/Baseline-1.3.6-jar-with-dependencies.jar" --help

Usage: Stellar ER Baseline(1.3.6) [options]
  Options:
    --cli, -c
      To run in cmd mode, use -c [dataset]
    --client, -cl
      A rest client sends a rest request, use -c [url]. It must combine with
      option --json
    --help, -h

    --json, -j
      To load a json request in client mode, use -j [config.json].
    --rest, -r
      To start rest server mode
      Default: false
```
3.To make ER work on datasets in CMD mode

```bash
java -jar "target/Baseline-1.3.6-jar-with-dependencies.jar" -c datasets/ACM_DBLP/param.json
```
4.To run ER in REST-API mode

```bash
java -jar "target/Baseline-1.3.6-jar-with-dependencies.jar" -r
```

5.To send a REST request to ER REST Server

```bash
java -jar "target/Baseline-1.3.6-jar-with-dependencies.jar" -cl http://url:7000/deduplicate -j datasets/ACM_DBLP/param.json
```

## REST API
1. Get ER REST version
````
version: GET /version
````
2. Send a entity resolution job to ER
````
deduplicate: POST /deduplicate/:json_param
````
3. Example of json parameters for `deduplicate` in dataset/Cora_Terror/param.json

## System Requirements
1. Oracle JDK 8
2. MAVEN 3


## License(s)

### Apache 2.0

Copyright 2016 CSIRO Data61

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.