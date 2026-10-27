package soys.soysmonthlycard.spring.entity.vo;

import java.util.Map;

/**
 * 设置视图 VO（{@code GET /admin/settings}）。
 *
 * <p>{@code settings} / {@code backends} / {@code mirror} 为动态配置回显段，
 * 保留 {@code Map} 承载（不回传数据库密码等敏感连接信息），不属"临时 VO"。</p>
 */
public class SettingsViewVO {
    private Map<String, Object> settings;
    private Map<String, Object> backends;
    private Map<String, Object> mirror;

    public Map<String, Object> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }

    public Map<String, Object> getBackends() {
        return backends;
    }

    public void setBackends(Map<String, Object> backends) {
        this.backends = backends;
    }

    public Map<String, Object> getMirror() {
        return mirror;
    }

    public void setMirror(Map<String, Object> mirror) {
        this.mirror = mirror;
    }
}
