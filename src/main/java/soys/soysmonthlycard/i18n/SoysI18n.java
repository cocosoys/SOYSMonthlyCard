package soys.soysmonthlycard.i18n;

import com.github.cocosoys.mc.soyshttpovermc.i18n.I18n;
import com.github.cocosoys.mc.soyshttpovermc.spi.Platforms;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * SOYSMonthlyCard 国际化门面：桥接主插件 SOYSHTTPOverMC 的 {@link I18n} 机制。
 *
 * <p><b>注册</b>：{@link #register(JavaPlugin)} 在 {@code onEnable} 时调用——
 * 把 {@code resources/language/zh_cn.yml}、{@code language/en_us.yml} 释放到
 * {@code plugins/SOYSMonthlyCard/language/}，再经主插件 {@link I18n#registerLanguageSource}
 * 注册为语言源（模板源：source 含 {@code {0}} 占位符，加载时替换为当前语言代码，
 * 一次注册即可适配 zh_cn / en_us 及后续新增语言，合并进默认作用域后立即重载生效）。</p>
 *
 * <p><b>取值</b>：{@link #t(String, String, Object...)} 直接委托 {@link I18n#t}，
 * key 统一 {@code soysmonthlycard.} 前缀避免与主插件默认作用域键冲突；未命中 key 时
 * 回退 fallback（即内联原文），天然兜底。</p>
 *
 * <p><b>独立性</b>：本插件自身的消息读取仍走 {@code language/<代码>.yml} 独立加载
 * （{@code SOYSMonthlyCard.getMessages()}），不依赖框架；注册语言源仅为让
 * {@code /soyshttp lang sources} 等框架能力可识别本插件语言包。</p>
 */
public final class SoysI18n {

    /** 语言源名称（/soyshttp lang sources 展示用） */
    private static final String SOURCE_NAME = "soysmonthlycard";
    /** 语言源描述 */
    private static final String SOURCE_DESC = "SOYSMonthlyCard 月卡消息";

    private SoysI18n() {
    }

    /**
     * 注册 SOYSMonthlyCard 语言包到 SOYSHTTPOverMC 语言资源（幂等：重复注册同名源，
     * 主插件按注册顺序覆盖同名键，最终文本一致）。
     *
     * @param plugin 本插件主类（用于定位语言文件夹）
     */
    public static void register(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        // 释放 jar 内语言资源 → plugins/SOYSMonthlyCard/language/*.yml（已存在则不覆盖，保留用户修改）
        saveLanguageResource(plugin, "zh_cn.yml");
        saveLanguageResource(plugin, "en_us.yml");

        // 注册语言源（模板源）：source 绝对路径含 {0} 占位符，加载时替换为当前语言代码
        File langDir = new File(plugin.getDataFolder(), "language");
        String source = new File(langDir, "{0}.yml").getAbsolutePath();
        I18n.registerLanguageSource(Platforms.getOrNull(), SOURCE_NAME, SOURCE_DESC, "", source);
    }

    private static void saveLanguageResource(JavaPlugin plugin, String fileName) {
        try {
            plugin.saveResource("language/" + fileName, false);
        } catch (IllegalArgumentException e) {
            // jar 内无该资源（异常情况）：跳过释放，后续注册仍可用已存在的文件
        }
    }

    /**
     * 取翻译文本（默认作用域，key 须带 soysmonthlycard. 前缀）；未命中回退 fallback。
     *
     * @param key      i18n 键（如 {@code soysmonthlycard.claim.success}）
     * @param fallback 回退文本（内联原文，未命中/语言包未加载时使用）
     * @param args     {@code {0} {1}...} 占位符参数
     */
    public static String t(String key, String fallback, Object... args) {
        return I18n.t(key, fallback, args);
    }
}
