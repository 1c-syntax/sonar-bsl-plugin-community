#!/usr/bin/env sh

./gradlew build localDeploy
curl -u admin:admin -X POST http://localhost:9000/api/system/restart
