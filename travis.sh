#!/usr/bin/env bash

set -e
set -o pipefail

sudo apt-get update && sudo apt-get install oracle-java8-installer
java -version

./mvnw -v
travis_wait ./mvnw -Dignore.random.failures=true clean verify -Ptravis  | grep -v Download
