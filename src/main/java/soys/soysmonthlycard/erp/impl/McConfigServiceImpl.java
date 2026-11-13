package soys.soysmonthlycard.erp.impl;

import com.github.cocosoys.mc.soyshttpovermc.platform.PlatformYaml;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.ConfigUtil;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.entity.MonthlyCardConfigFile;
import soys.soysmonthlycard.erp.entity.vo.McConfigFileVO;
import soys.soysmonthlycard.erp.entity.vo.McConfigSaveVO;
import soys.soysmonthlycard.erp.service.McConfigService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 「插件配置」文件编辑实现：目标目录 = owner 插件（SOYSMonthlyCard）自己的数据目录。
 *
 * <p>自包含：构造器注入 owner 实例，按 {@code plugin.getDataFolder()} 解析，
 * 不写死任何插件名、不通过 PluginManager 去查找别的插件目录。</p>
 *
 * <p>读写复用平台层：{@link PlatformYaml#load} / {@link PlatformYaml#save}（统一接管编码，
 * 低版本 UTF-8 不乱码）+ {@link ConfigUtil#toMap} / {@link ConfigUtil#toYaml}
 * （YAML ↔ Map 递归转换）。全量写回，<b>注释不保留</b>。</p>
 *
 * <p>所有方法以 {@code try/catch(Throwable)} 收口，异常转成 {@code AjaxResult.error(msg)}，
 * 不让堆栈穿透到 HTTP 层。</p>
 */
public class McConfigServiceImpl implements McConfigService {

    private final SOYSMonthlyCard plugin;

    public McConfigServiceImpl(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public AjaxResult list() {
        try {
            List<McConfigFileVO> rows = new ArrayList<>();
            for (MonthlyCardConfigFile f : MonthlyCardConfigFile.values()) {
                McConfigFileVO vo = new McConfigFileVO();
                vo.setId(f.getId());
                vo.setName(f.getName());
                vo.setFile(f.getFile());
                vo.setDesc(f.getDesc());
                rows.add(vo);
            }
            return AjaxResult.success(rows);
        } catch (Throwable t) {
            return AjaxResult.error("配置清单查询失败：" + t.getMessage());
        }
    }

    @Override
    public AjaxResult load(String fileId) {
        try {
            MonthlyCardConfigFile f = requireFile(fileId);
            File file = resolve(f);
            if (!file.isFile()) {
                return AjaxResult.error("配置文件不存在，请先启动一次插件生成默认配置: " + file.getPath());
            }
            return AjaxResult.success(ConfigUtil.toMap(PlatformYaml.load(file)));
        } catch (Throwable t) {
            return AjaxResult.error("配置读取失败：" + t.getMessage());
        }
    }

    @Override
    public AjaxResult save(String fileId, Map<String, Object> data) {
        try {
            MonthlyCardConfigFile f = requireFile(fileId);
            File file = resolve(f);
            if (!file.isFile()) {
                return AjaxResult.error("配置文件不存在，请先启动一次插件生成默认配置: " + file.getPath());
            }
            if (data == null) {
                return AjaxResult.error("配置内容为空");
            }
            PlatformYaml.save(ConfigUtil.toYaml(data), file);
            McConfigSaveVO resp = McConfigSaveVO.of(f.getFile(),
                    "已保存。执行 /monthlycard reload 热重载生效（存储后端 / 连接串等变更需重启服务器）");
            return AjaxResult.success(resp);
        } catch (Throwable t) {
            return AjaxResult.error("配置保存失败：" + t.getMessage());
        }
    }

    // ===== 内部 =====

    private static MonthlyCardConfigFile requireFile(String fileId) {
        MonthlyCardConfigFile f = MonthlyCardConfigFile.byId(fileId);
        if (f == null) {
            throw new IllegalArgumentException("未知配置文件 id: " + fileId);
        }
        return f;
    }

    /** owner 插件自己的数据目录下解析目标文件。 */
    private File resolve(MonthlyCardConfigFile f) {
        if (plugin == null) {
            throw new IllegalStateException("owner 插件实例未注入");
        }
        return new File(plugin.getDataFolder(), f.getFile());
    }
}
