package cn.sensorsdata.sample;

import com.sensorsdata.analytics.extractor.processor.ExtProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by fengjiajie on 17/1/22.
 */
public class ExtProcessorFindNewUser implements ExtProcessor {

  // 需要处理的项目
  private static final String[] PROCESS_PROJECT = {"production", "default"};
  // redis 服务地址
  static final String REDIS_HOST = "10.10.163.96";
  // redis 服务端口号
  static final int REDIS_PORT = 6379;
  // redis 密码, 若无密码使用 null 即可
  static final String REDIS_PASSWORD = null;
  // redis 数据库, 默认 0
  static final int REDIS_DB_INDEX = 0;
  static final int JEDIS_POOL_TIMEOUT = 6000;
  static final int JEDIS_POOL_MAX_IDLE = 4;
  static final int JEDIS_POOL_MIN_IDLE = 1;

  private static ObjectMapper objectMapper = new ObjectMapper();
  private static JedisPool jedisPool;
  private static Set<String> processProjectSet;
  private static final Logger logger = LoggerFactory.getLogger(ExtProcessorFindNewUser.class);

  public ExtProcessorFindNewUser() {
    synchronized (ExtProcessorFindNewUser.class) {
      // 创建 redis 连接池
      if (jedisPool == null) {
        // 配置连接池
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxWaitMillis(JEDIS_POOL_TIMEOUT);
        jedisPoolConfig.setMaxIdle(JEDIS_POOL_MAX_IDLE);
        jedisPoolConfig.setMinIdle(JEDIS_POOL_MIN_IDLE);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig, REDIS_HOST, REDIS_PORT, JEDIS_POOL_TIMEOUT,
            REDIS_PASSWORD, REDIS_DB_INDEX);

        processProjectSet =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PROCESS_PROJECT)));
      }
    }
  }

  private void processFindNewUser(JsonNode recordNode) {
    // 从数据中提取 project 字段, 用于判断这条数据是否需要处理
    String project = "default";
    if (recordNode.has("project")) {
      project = recordNode.get("project").asText();
    }

    // 判断是否是需要处理的项目
    if (!processProjectSet.contains(project)) {
      return;
    }

    // 取 event 字段
    JsonNode eventNode = recordNode.get("event");
    // 需要判断是否为空, 如用户属性数据没有 event 字段
    if (eventNode == null) {
      return;
    }
    String eventName = eventNode.asText();
    // 判断是否是需要处理的事件, 例如需要处理的事件是 AppStartEvent
    if (!"AppStartEvent".equals(eventName)) {
      return;
    }

    // 取 time 字段
    String timeField;
    JsonNode timeNode = recordNode.get("time");
    if (timeNode != null) {
      timeField = timeNode.asText();
    } else {
      timeField = Long.toString(System.currentTimeMillis());
    }

    // 取 distinct_id 字段
    String distinctId;
    JsonNode distinctIdNode = recordNode.get("distinct_id");
    // 如果没有 distinct_id 字段, 不处理
    if (distinctIdNode == null) {
      return;
    }
    distinctId = distinctIdNode.asText();

    // 按 {项目名}-{distinct_id} 拼出 redis key, 根据 key 判断是否是新用户
    String redisKey = project + '-' + distinctId;

    boolean isNewUser = false;
    while (true) {
      try (Jedis jedis = jedisPool.getResource()) {
        if (jedis.setnx(redisKey, timeField) == 1L) {
          // SETNX 如果成功, 那么说明 redis 中之前没有该 ID, 该用户为新用户
          isNewUser = true;
        } else {
          // 一条数据可能会多次经过 ExtProcessor (如模块重启可能将之前处理过的数据再次处理)
          // SETNX 失败, 说明 redis 中已经存在该 ID, 通过 value 判断是否是之前的 SETNX 那条数据再次触发
          if (timeField.equals(jedis.get(redisKey))) {
            // value 的值与 SETNX 的 value 相同, 说明是新用户
            isNewUser = true;
          }
        }
        break;
      } catch (Exception e) {
        logger.warn("Process failed, key: " + redisKey + ", val: " + timeField, e);
        try {
          Thread.sleep(1000 * 10);
        } catch (InterruptedException e1) {
          // Interrupted
          return;
        }
      }
    }

    ObjectNode propertiesNode = (ObjectNode) recordNode.get("properties");
    if (propertiesNode != null) {
      // 将结果添加到 properties 里
      propertiesNode.put("NewUserByExtProcessor", isNewUser);
    }
  }

  @Override public String process(String record) throws Exception {
    JsonNode recordNode = objectMapper.readTree(record);

    try {
      processFindNewUser(recordNode);
    } catch (Exception e) {
      logger.warn("Process with Exception, record: " + record, e);
    }

    return recordNode.toString();
  }
}
