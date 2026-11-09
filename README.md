# BookCache · 图书查询缓存优化（Spring Boot + Redis）

基于 **Spring Boot 2.7.5 + MyBatis + MySQL + Redis + Thymeleaf** 的图书查询系统，
用 Redis 缓存「高频访问、低变更」的图书数据，降低 MySQL 查询压力，演示 **Cache-Aside（旁路缓存）** 的完整落地。

## 背景

图书首页属于典型的「读多写少」场景：用户每刷新一次页面，系统就向 MySQL 发起一次全表查询。
并发上来之后，MySQL 压力集中在同一批热点数据上，页面响应明显变慢。

本项目的做法是：在 Service 层加一层 Redis 缓存，**读请求先走缓存，未命中再查库并回填**，
把重复查询挡在数据库之前。

## 技术栈

| 层次 | 技术 |
|---|---|
| 框架 | Spring Boot 2.7.5（Java 8） |
| 持久层 | MyBatis 2.2.2（注解式 `@Select`） |
| 数据库 | MySQL 5.7（`mysql-connector-java` 5.1.49） |
| 缓存 | Spring Data Redis（`RedisTemplate`） |
| 视图 | Thymeleaf + jQuery 异步渲染 |
| 构建 | Maven |

## 核心设计

### 1. 缓存 Key 规范

```java
private static final String BOOK_SINGLE_KEY_PREFIX = "book:";     // book:{id}
private static final String BOOK_ALL_LIST_KEY     = "book:list:all";
```

单本与全量列表分开存放，避免「更新一本书导致整表缓存失效」。

### 2. Cache-Aside 读流程

```
请求 → Service
        ├─ 命中 Redis  → 直接返回
        └─ 未命中      → 查 MySQL → 写回 Redis → 返回
```

`BookServiceImpl` 中分别实现了 `getBookById()` 与 `getAllBooks()` 两条链路，
并通过控制台日志区分「命中缓存 / 缓存失效 / 缓存预热」三种状态，便于验证效果。

### 3. 序列化配置

默认的 JDK 序列化在 Redis 里会存成乱码，无法直接排查数据。
`RedisConfig` 中自定义了 `RedisTemplate`：

- **Key**：`StringRedisSerializer`
- **Value**：`Jackson2JsonRedisSerializer` + `ObjectMapper`（`activateDefaultTyping(NON_FINAL)` 保留类型信息）

这样 Redis 里存的是可读 JSON，`redis-cli` 能直接看到内容。

### 4. 过期与穿透防护

- 缓存统一 TTL：**600 秒**（`RedisCacheConfiguration.entryTtl`）
- `disableCachingNullValues()`：不缓存空值，避免「查不到的数据反复打库」的缓存穿透
- `CacheServiceImpl` 中手动写入的缓存另设 **1 小时** TTL

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/` | 首页（Thymeleaf 渲染表格） |
| GET | `/api/books` | 查询全部图书（走缓存） |
| GET | `/book/{id}` | 按 ID 查询单本图书（走缓存） |

## 运行方式

### 1. 准备数据库

```sql
CREATE DATABASE book_db DEFAULT CHARACTER SET utf8mb4;

USE book_db;

CREATE TABLE book (
  id           INT PRIMARY KEY AUTO_INCREMENT,
  name         VARCHAR(100)  NOT NULL COMMENT '书名',
  author       VARCHAR(50)   COMMENT '作者',
  publisher    VARCHAR(100)  COMMENT '出版社',
  publish_date DATE          COMMENT '出版日期',
  price        DECIMAL(10,2) COMMENT '单价',
  stock        INT           COMMENT '总数'
);
```

### 2. 修改配置

`src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/book_db?useUnicode=true&characterEncoding=utf-8&useSSL=false
spring.datasource.username=root
spring.datasource.password=你的密码
spring.redis.host=localhost
spring.redis.port=6379
```

> 仓库中的配置已做脱敏处理，请填入你自己的本地地址与密码。

### 3. 启动

```bash
mvn spring-boot:run
```

访问 <http://localhost:8080>

### 4. 验证缓存是否生效

1. 首次访问 `/api/books`，控制台输出：`>>> [列表缓存失效] 查询MySQL全部图书`
2. 再次访问，控制台输出：`>>> [列表命中缓存] 直接读取Redis全部图书数据`
3. 用 `redis-cli` 查看：`get book:list:all`，可以看到 JSON 格式的缓存内容

## 已知问题 / 后续优化

- [ ] **缓存一致性**：目前只有读逻辑，写操作未做「先更新库、再删除缓存」，列表缓存会脏到 TTL 过期
- [ ] 缓存穿透已用「不缓存 null」缓解，但未做布隆过滤器
- [ ] 热点 Key 未做本地缓存（Caffeine）二级缓存，可以进一步减少 Redis 网络往返
- [ ] `@EnableCaching` 已开启但业务代码走的手动缓存，`@Cacheable` 注解方式未实际使用，两者可以统一
- [ ] 缺少单元测试与压测数据（可用 JMeter 对比加缓存前后的 QPS 与平均响应时间）

## 目录结构

```
bookcache_swj215
├── src/main/java/edu/hou/cache
│   ├── CacheApplication.java        # 启动类
│   ├── bean/Book.java               # 实体类
│   ├── config/RedisConfig.java      # RedisTemplate 与 CacheManager 配置
│   ├── controller/IndexController.java  # 页面与 REST 接口
│   ├── mapper/BookMapper.java       # MyBatis 注解式 Mapper
│   └── service/                     # 业务层 + 缓存层
└── src/main/resources
    ├── application.properties
    ├── static/                      # css / jquery
    └── templates/index.html         # 首页
```

## License

课程项目，仅供学习交流。
