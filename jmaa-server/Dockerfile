# 基础镜像使用java
#FROM openjdk:8-jdk-alpine
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:8-jdk-alpine3.9

# UTF-8 配置
RUN apk add --no-cache tzdata locales \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.conf \
    && locale-gen en_US.UTF-8

ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    TZ=Asia/Shanghai

ARG JAR_FILE=target
ARG active_env

ADD  ${JAR_FILE} /jmaa

# 运行端口
EXPOSE 8066


#ENTRYPOINT ["java","-jar","/jmaa/jmaa-server-1.0.jar"]
ENTRYPOINT ["/bin/sh", "-c", "set -e && java -XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=512m -Xms8192m -Xmx8192m -Xmn4096m -XX:SurvivorRatio=8 -XX:NewRatio=2 -XX:+UseConcMarkSweepGC -jar /jmaa/jmaa-server-1.0.jar"]
