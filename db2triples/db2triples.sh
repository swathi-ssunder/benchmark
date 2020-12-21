#!/bin/bash

java -cp target/db2triples-1.0-SNAPSHOT.jar:target/dependency/* com.kg.benchmark.Db2Triples $@
