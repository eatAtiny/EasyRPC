# EasyRPC - 轻量级Java RPC框架

EasyRPC是一个轻量级、高性能的Java RPC框架，支持多种通信协议、序列化方式和注册中心。框架设计简洁，易于扩展，适合学习和生产环境使用。

## 项目特性

- **多协议支持**：支持HTTP和TCP两种通信协议
- **多种序列化方式**：支持JDK、JSON、Hessian、Kryo、Protobuf等多种序列化协议
- **服务注册与发现**：支持Etcd、ZooKeeper、Redis、本地注册等多种注册中心
- **负载均衡**：支持随机、轮询、最少连接、一致性哈希等负载均衡策略
- **容错机制**：支持重试策略和容错策略
- **SPI扩展机制**：基于Java SPI机制，易于扩展新功能
- **Spring Boot集成**：提供Spring Boot Starter，方便集成到Spring项目中

## 项目结构

```
EasyRPC/
├── easy-rpc/                    # 核心框架模块
│   ├── src/main/java/com/easy/simple/rpc/
│   │   ├── annotation/          # 注解定义
│   │   ├── bootstrap/           # 启动引导类
│   │   ├── config/              # 配置类
│   │   ├── enity/               # 实体类（请求/响应/服务元信息）
│   │   ├── fault/               # 容错机制（重试/容错策略）
│   │   ├── loadbalance/         # 负载均衡策略
│   │   ├── protocol/            # 协议编解码
│   │   ├── proxy/               # 代理类（服务代理/请求发送器）
│   │   ├── registry/            # 注册中心
│   │   ├── serializer/          # 序列化器
│   │   ├── server/               # 服务器实现
│   │   └── utils/                # 工具类
│   └── pom.xml
├── example-common/              # 示例公共模块
├── example-consumer/            # 示例消费者模块
├── example-provider/            # 示例提供者模块
├── spring-consumer/              # Spring Boot消费者示例
└── spring-provider/              # Spring Boot提供者示例
```

## 快速开始

### 1. 环境要求

- JDK 11+
- Maven 3.6+

### 2. 编译项目

```bash
# 编译整个项目
mvn clean compile

# 打包项目
mvn clean package
```

### 3. 运行示例

#### 基础示例

1. **启动服务提供者**
```bash
cd example-provider
mvn exec:java -Dexec.mainClass="com.easy.simple.rpc.example.EasyProviderExample"
```

2. **启动服务消费者**
```bash
cd example-consumer
mvn exec:java -Dexec.mainClass="com.easy.simple.rpc.example.EasyConsumerExample"
```

#### Spring Boot示例

1. **启动Spring Boot服务提供者**
```bash
cd spring-provider
mvn spring-boot:run
```

2. **运行Spring Boot服务消费者测试**
```bash
cd spring-consumer
mvn test
```

或者直接在IDE中运行测试类：
```java
// 测试类路径：src/test/java/com/easy/rpc/springconsumer/SpringConsumerApplicationTests.java
@SpringBootTest
class SpringConsumerApplicationTests {
    @Resource
    private ExampleServiceImpl exampleServiceImpl;
    
    @Test
    void test() {
        exampleServiceImpl.test();
    }
}
```

## 配置说明

### 配置文件格式

支持YAML和Properties两种格式的配置文件：

**application.yml**
```yaml
rpc:
  name: easy-rpc
  version: 1.0.0
  serverPort: 8080
  serializer: json
  registry:
    registry: etcd
    address: http://localhost:2379
  loadbalancer: random
  retry: fixed
  tolerant: fail-fast
  serverType: tcp
```

**application.properties**
```properties
rpc.name=easy-rpc
rpc.version=1.0.0
rpc.serverPort=8080
rpc.serializer=json
rpc.registry.registry=etcd
rpc.registry.address=http://localhost:2379
rpc.loadbalancer=random
rpc.retry=fixed
rpc.tolerant=fail-fast
rpc.serverType=tcp
```

### 配置项说明

| 配置项 | 说明 | 可选值 | 默认值 |
|--------|------|--------|--------|
| rpc.version | 应用版本 | 任意字符串 | 1.0.0 |
| rpc.serverPort | 服务端口 | 1024-65535 | 8080 |
| rpc.serializer | 序列化方式 | jdk, json, hessian, kryo, protobuf | jdk |
| rpc.registry.registry | 注册中心类型 | etcd, zookeeper, redis, local | local |
| rpc.registry.address | 注册中心地址 | 对应注册中心地址 | - |
| rpc.loadbalancer | 负载均衡策略 | random, roundRobin, leastConn, consistentHash | random |
| rpc.retry | 重试策略 | no, fixed | no |
| rpc.tolerant | 容错策略 | fail-fast, fail-over, fail-safe, fail-back | fail-fast |
| rpc.serverType | 服务器类型 | tcp, http | tcp |

## 核心功能

### 1. 服务定义

在服务提供者端定义服务接口：

```java
public interface UserService {
    User getUserById(Long id);
    List<User> getUsers();
}
```

### 2. 服务实现

```java
@RpcService
public class UserServiceImpl implements UserService {
    @Override
    public User getUserById(Long id) {
        return new User(id, "test-user");
    }
    
    @Override
    public List<User> getUsers() {
        return Arrays.asList(new User(1L, "user1"), new User(2L, "user2"));
    }
}
```

### 3. 服务消费

```java
@RpcReference
private UserService userService;

public void testRpc() {
    User user = userService.getUserById(1L);
    System.out.println("获取用户: " + user);
}
```

### 4. Spring Boot集成

在Spring Boot应用中，使用注解即可快速集成：

```java
@SpringBootApplication
@EnableRpc // 开启 RPC 功能 (needServer = false 表示仅作为消费者模式运行，不开启服务器，默认为true)
public class ConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }
}
```

## 扩展机制

### SPI扩展

EasyRPC基于Java SPI机制，支持以下扩展点：

1. **序列化器**：实现`Serializer`接口
2. **注册中心**：实现`Registry`接口  
3. **负载均衡器**：实现`LoadBalancer`接口
4. **重试策略**：实现`RetryStrategy`接口
5. **容错策略**：实现`TolerantStrategy`接口
6. **服务器**：实现`WebServer`或相关接口

### 自定义扩展示例

以自定义序列化器为例：

1. 实现`Serializer`接口
```java
public class CustomSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T object) {
        // 自定义序列化逻辑
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        // 自定义反序列化逻辑
    }
}
```

2. 在`META-INF/services/`目录下创建SPI配置文件

## 性能优化

- **连接复用**：TCP连接支持复用，减少连接建立开销
- **异步处理**：基于Vert.x的异步非阻塞IO
- **协议优化**：自定义紧凑协议，减少网络传输开销
- **缓存机制**：服务发现结果缓存，减少注册中心查询

## 监控与调试

框架提供以下调试信息：
- 服务注册/注销日志
- 请求/响应日志
- 异常堆栈信息
- 性能指标统计

## 故障排除

### 常见问题

1. **服务注册失败**
   - 检查注册中心连接配置
   - 确认注册中心服务正常运行

2. **服务调用超时**
   - 检查网络连通性
   - 调整超时时间配置
   - 检查服务提供者状态

3. **序列化异常**
   - 确认序列化方式一致
   - 检查实体类版本兼容性

### 日志配置

框架使用SLF4J + Logback日志框架，可通过`logback.xml`配置日志级别：

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.easy.simple.rpc" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

## 联系方式

如有问题或建议，请通过以下方式联系：
- 提交Issue
- 发送邮件

---

**EasyRPC 文档**

https://pppr8ikl5f.feishu.cn/wiki/ThgcwtuXgibN8lkyKV7cwPd1nog
