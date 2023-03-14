#!/bin/bash

set -ex

REGISTRY="arvindnr"
IMAGE_NAME="searchapi-cartup"
TAG="${1}"
dir="$(pwd)/Dockerfile-prod"


git checkout production
git pull

rm -rf conf/datastoreconfig.properties
mvn clean
mvn package

echo "pwd: $(pwd)"
docker build -t ${REGISTRY}/${IMAGE_NAME}:v${TAG} -t ${REGISTRY}/${IMAGE_NAME}:latest -f ${dir} .
docker push ${REGISTRY}/${IMAGE_NAME}
docker push ${REGISTRY}/${IMAGE_NAME}:v${TAG}