#!/bin/bash

set -ex

REGISTRY="arvindnr"
IMAGE_NAME="searchapi-staging"
BUILD="${1}"
DEPLOY="${2}"
dir="$(pwd)/Dockerfile-stage"
b=1

echo "pwd: $(pwd)"


if [ "${BUILD}" = 1 ]
then
	NEWTAG="${3}"
	git checkout staging
	git pull

	rm -rf conf/datastoreconfig.properties
	mvn clean
	mvn package -DskipTests
	docker build -t ${REGISTRY}/${IMAGE_NAME}:v${NEWTAG} -t ${REGISTRY}/${IMAGE_NAME}:latest -f ${dir} .
	docker push ${REGISTRY}/${IMAGE_NAME}
	docker push ${REGISTRY}/${IMAGE_NAME}:v${NEWTAG}
else
	echo "Not building"
fi

if [ "${DEPLOY}" = 1 ]
then
	NEWTAG="${3}"
	docker rm -f ${IMAGE_NAME}
	docker run -d -p 8093:8080 --name ${IMAGE_NAME} -v /opt/docker/volumes/${IMAGE_NAME}:/tomcat/logs ${REGISTRY}/${IMAGE_NAME}:v${NEWTAG}
else
	echo "skipping deploy"
fi
