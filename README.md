Stellar-ERBaseline
====================

# Introduction

This repository hosts stellar-ERBaseline, a module of the [Stellar - Graph Analytics platform](https://github.com/data61/stellar) developed by [CSIRO Data61](https://data61.csiro.au). This module performs a baseline entity resolution function on graphs ingested to the Stellar platform.

If you are interested in running the entire Stellar platform, please refer to the instructions on the main Stellar [repository](https://github.com/data61/stellar).

# Overview

The Stellar-ERBaseline module is developed based on the (SERF framework)[http://infolab.stanford.edu/serf/], with an extension of a simple blocking mechanism. The blocking mechanism adopts a redundancy negative approach, where records are clustered into the same block based on their textual similarity of a selected properties. We use locality sensitive hashing (LSH) to map similar records into the same block. Each record is assigned to only one block. Within the same block, similar records are being merged based on the textual similarity of selected attributes using Jaro-Wrinkler TF-IDF approach. Different configurations of textual similarity thresholds and blocking thresholds result in a different entity resolution.

The configuration can be changed under datasets/[Dataset folder]/param.json. For example, for the ACM_DBLP dataset, changing the following attributes may produce a different set of results:

- “attributes”:{“address”:0.6, “full_name”:0.9},
- “options”:{“bucketsize”:1, “similarity”:0.9, “seed”:1234, “ground_truth”:“DBLP-ACM_perfectMapping.csv”}


The input parameters are described as follows:
- address - input value [0..1]  -
The textual similarity of the “address” attribute in two records must be at least above this threshold in order to be classified as a similar record.

- full_name - input value [0..1]  -
The textual similarity of the “name” attribute in two records must be at least above this threshold in order to be classified as a similar record.

- bucketsize - input value [1-1000] -
Number of blocks to create in the simple blocking mechanism.

- similarity - input value [0…1] -
Similarity error of the hash function in LSH.

- seed - input value [-1, 0…N] -
As the parameters of the hashing function are randomly initialized when the LSH object is instantiated, set the “seed” to a positive number to produce comparable signatures, set to -1 otherwise.

- ground_truth - optional input -
The module calculates Precision/Recall/F-score of the entity resolution based on this ground truth.


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