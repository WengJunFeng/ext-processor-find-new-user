import java.util.Set;

/**
 * Created by fengjiajie on 17/1/22.
 */
public class UserIdSet {

  Set<String> getExistedIdSet() {
    return null;
  }

  boolean isNewUser(String distinctId) {
    // 获取已出现 ID 集合
    Set<String> existUserIdSet = getExistedIdSet();
    // 如果不在集合中, 那么是新用户
    if (!existUserIdSet.contains(distinctId)) {
      // 将 ID 加入到已出现集合里
      existUserIdSet.add(distinctId);
      return true;
    }
    return false;
  }

  public static void main(String[] args) {

  }

}
