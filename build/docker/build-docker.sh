#!/bin/bash
set -x

image=$1

rm -rf conf/datastoreconfig.properties

if [ $2 = "stage" ]; then
    cp conf/datastoreconfig-stage.properties conf/datastoreconfig.properties
    dir="$(pwd)/build/docker/Dockerfile-stage"
    image=${image}-stage
elif [ $2 = "prod" ]; then
    cp conf/datastoreconfig-prod.properties conf/datastoreconfig.properties
    dir="$(pwd)/build/docker/Dockerfile-prod"
    image=${image}-prod
elif [ $2 = "prod-us" ]; then
    cp conf/datastoreconfig-prod-us.properties conf/datastoreconfig.properties
    dir="$(pwd)/build/docker/Dockerfile-prod-us"
    image=${image}-prod-us
elif [ $2 = "dev" ]; then
    cp conf/datastoreconfig-dev.properties conf/datastoreconfig.properties
    dir="$(pwd)/build/docker/Dockerfile-dev"
    image=${image}-dev 
else
    exit
fi

mvn clean
mvn package -DskipTests
docker build -t $image:$3 -f ${dir} .

if [ $4 = "push" ]; then 
    docker push $image:$3
fi