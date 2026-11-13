package soys.soysmonthlycard.erp.service;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.Map;

/**
 * 「插件配置」菜单服务：配置文件清单 / 读取 / 保存。
 */
public interface McConfigService {

    /** 可编辑配置文件清单。 */
    AjaxResult list();

    /** 读取指定配置文件，返回 YAML → Map 的完整结构。 */
    AjaxResult load(String fileId);

    /** 全量写回指定配置文件。 */
    AjaxResult save(String fileId, Map<String, Object> data);
}
