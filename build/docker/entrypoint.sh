#export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/apr/lib
export JAVA_HOME=/usr/lib/jvm/java-8-oracle
export PATH=$JAVA_HOME/bin:$PATH

JVM_PARAM=" -Xms1024m -Xmx2048m "
JVM_PARAM="${JVM_PARAM} -Djava.security.egd=file:/dev/urandom "

if [ "$DEPLOY_ENV" == "dev" ]; then
    JVM_PARAM="${JVM_PARAM} -Xdebug -Xrunjdwp:server=y,transport=dt_socket,suspend=n "
fi

APM_PARAM=" -javaagent:/conf/apm-agent.jar \
-Delastic.apm.service_name=widgetapi-staging \
-Delastic.apm.server_urls=http://10.110.0.36:8200 \
-Delastic.apm.secret_token= \
-Delastic.apm.environment=staging \
${JVM_PARAM}"

export JAVA_OPTS=$APM_PARAM

# use this logging to console, default is logging to file
export CONF_BASE_PATH=/conf/
export CACHE_CONF_BASE_PATH=/conf/
java $JAVA_OPTS -jar -Dserver.tomcat.basedir=tomcat -Dserver.tomcat.accesslog.enabled=true /cartup-search-service.war
