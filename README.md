# SpringCloud 全家桶 Demo

##### 由于文件太大，自行下载nacos：https://github.com/alibaba/nacos/releases
##### 启动nacos后将 ./nacos/export_config/ 下的配置文件导入nacos中，具体关于nacos启动请看./nacos/README.md

#### 整体项目结构：
```
├── README.md
 ├── consumer (服务调用方)
 │   ├── pom.xml
 │   └── src
 │       ├── main
 │       │   ├── java
 │       │   └── resources
 │       └── test
 ├── dal (数据库访问层)
 |   ├── mybatis-gen
 │   ├── pom.xml
 │   └── src
 │       ├── main
 │       │   ├── java
 │       │   └── resources
 │       └── test
 ├── nacos (alibaba第二代微服务注册中心和配置中心，代替eureka)
 │   ├── export_config (导出的配置)
 |
 ├── gateway (gateway网关 推荐使用，采用了webflux比zuul性能高)
 │   ├── pom.xml
 │   └── src
 │       ├── main
 │       │   ├── java
 │       │   └── resources
 │       └── test
 ├── hystrix-dashboard (hystrix 熔断监控)
 │   ├── pom.xml
 │   └── src
 │       ├── main
 │       │   ├── java
 │       │   └── resources
 │       └── test
 ├── producer  (服务服务提供方)
 │   ├── pom.xml
 │   ├── producer-api (服务服务提供方的api)
 │   │   ├── pom.xml
 │   │   └── src
 │   │       ├── main
 │   │       └── test
 │   └── producer-service
 │       ├── pom.xml
 │       └── src
 │           ├── main
 │           └── test
 ├── fe (前端)
 │   ├── index.html
 │   └── node-server.js (node前端服务)
 |
 ├── eureka-server (eureka服务注册中心) -------> (已被nacos替换)
 │   ├── pom.xml
 │   └── src
 │       ├── main
 │       │   ├── java
 │       │   └── resources
 │       └── test
 └── zuul  (zuul 网关)  -------> (已被gateway替换)
 |   ├── pom.xml
 |   └── src
 |       ├── main
 |       │   ├── java
 |       │   └── resources
 |       └── test
```
