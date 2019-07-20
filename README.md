# graceful-offline-spring-cloud-starter
spring cloud micro-service graceful offline tool

## 简介

graceful-offline-spring-cloud-starter是一个实现Spring Cloud体系微服务优雅退出的工具。每个微服务引入此starter后，会通过actuactor暴露2个端点：/check和
/gracefuloffline 。check端点用于判断当前微服务是否有使用某个微服务。gracefuloffline端点利用check端点实现判断其他微服务有无使用当前微服务，从而实现当前微服务的优雅退出。

## 背景

## 设计思路

## 使用

## 遗留问题
