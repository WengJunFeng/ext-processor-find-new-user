package cn.sensorsdata.sample;

import redis.clients.jedis.Jedis;

import java.util.Scanner;

/**
 * Created by fengjiajie on 17/1/22.
 */
public class ImportIds {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("First arg is project.");
      System.exit(1);
    }
    String project = args[0].trim();
    if (project.isEmpty()) {
      System.err.println("Empty project");
      System.exit(2);
    }

    Scanner scanner = new Scanner(System.in);
    Jedis jedis = new Jedis(ExtProcessorFindNewUser.REDIS_HOST, ExtProcessorFindNewUser.REDIS_PORT,
        ExtProcessorFindNewUser.JEDIS_POOL_TIMEOUT);
    if (ExtProcessorFindNewUser.REDIS_PASSWORD != null) {
      jedis.auth(ExtProcessorFindNewUser.REDIS_PASSWORD);
    }

    int count = 0;
    while (scanner.hasNext()) {
      String id = scanner.next();
      String key = project + '-' + id;
      String value = "";
      jedis.set(key, value);
      ++count;
    }

    jedis.close();
    System.out.println("Set " + count + " ids for project '" + project + "'");
  }
}
