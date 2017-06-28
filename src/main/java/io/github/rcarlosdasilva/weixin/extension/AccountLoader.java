package io.github.rcarlosdasilva.weixin.extension;

import java.util.List;

import io.github.rcarlosdasilva.weixin.model.Account;

/**
 * 微信公众号加载器
 *
 * @author Dean Zhao (rcarlosdasilva@qq.com)
 */
public interface AccountLoader {

  /**
   * 加载需要注册的微信公众号信息，加载后会自动注册.
   * <p>
   * 返回的Account中，Key就是注册公众号时使用的Key。
   * 
   * @return 公众号集合
   */
  List<Account> load();

}
