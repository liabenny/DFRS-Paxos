#!/bin/bash

docker rm -f process1

docker rm -f process2

docker rm -f process3

docker network rm dsa_network

docker network create --driver bridge dsa_network

docker create -t -i --name process1 -w /bin submittyrpi/csci4510:default /bin/bash

docker create -t -i --name process2 -w /bin submittyrpi/csci4510:default /bin/bash

docker create -t -i --name process3 -w /bin submittyrpi/csci4510:default /bin/bash

docker network connect dsa_network process1

docker network connect dsa_network process2

docker network connect dsa_network process3

./build.sh

cp ./knownhosts.json ./bin/knownhosts.json
