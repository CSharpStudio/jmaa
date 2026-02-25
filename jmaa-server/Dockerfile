# 基础镜像使用java
#FROM openjdk:8-jdk-alpine
FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/openjdk:8-jdk-alpine3.9
# 作者 名字 邮箱 （可以不写）
MAINTAINER lv

ARG JAR_FILE=target
ARG active_env

ADD  ${JAR_FILE} /jmaa

# 运行端口
EXPOSE 6067

# 处理字体问题
# RUN apk add --update font-adobe-100dpi ttf-dejavu fontconfig

## 设置所属时区
ENV TZ=Asia/Shanghai
#ENV active_env=pro

#ENTRYPOINT ["java","-jar","/jmaa/jmaa-server-1.0.jar"]
ENTRYPOINT ["/bin/sh", "-c", "set -e && java -XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=512m -Xms8192m -Xmx8192m -Xmn4096m -XX:SurvivorRatio=8 -XX:NewRatio=2 -XX:+UseConcMarkSweepGC -jar /jmaa/jmaa-server-1.0.jar"]
