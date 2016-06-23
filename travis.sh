#!/usr/bin/env bash

set -e
set -o pipefail

sudo apt-get update && sudo apt-get install oracle-java8-installer
java -version


mvn -Dignore.random.failures=true clean verify -Pgithub-its  | grep -v Download
