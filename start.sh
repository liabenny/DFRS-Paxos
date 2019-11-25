#!/bin/bash

docker start $1

docker cp bin $1:bin

docker exec -it $1 /bin/bash
