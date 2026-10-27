package soys.soysmonthlycard.erp.impl;

import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import soys.soysmonthlycard.SOYSMonthlyCard;
import soys.soysmonthlycard.erp.entity.vo.McTierSaveVO;
import soys.soysmonthlycard.erp.service.McTierService;
import soys.soysmonthlycard.erp.util.McRewards;
import soys.soysmonthlycard.spring.entity.vo.RewardsVO;
import soys.soysmonthlycard.spring.entity.vo.TierDetailVO;
import soys.soysmonthlycard.spring.entity.vo.TierListRowVO;
import soys.soysmonthlycard.spring.entity.vo.TierToggleVO;
import soys.soysmonthlycard.web.ItemSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link McTierService} 实现：直接操作 rewards.yml（复用既有档位配置），不新增数据表。
 */
public class McTierServiceImpl implements McTierService {

    private final SOYSMonthlyCard plugin;

    public McTierServiceImpl(SOYSMonthlyCard plugin) {
        this.plugin = plugin;
    }

    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String keyword) {
        Map<UUID, List<String>> claimedMonth = plugin.getStorage().getClaimsThisMonth();
        List<TierListRowVO> rows = new ArrayList<>();
        ConfigurationSection tiers = McRewards.tiers(plugin);
        if (tiers != null) {
            String kw = keyword == null ? null : keyword.trim().toLowerCase();
            for (String key : tiers.getKeys(false)) {
                ConfigurationSection tier = tiers.getConfigurationSection(key);
                if (tier == null) {
                    continue;
                }
                if (kw != null && !kw.isEmpty() && !key.toLowerCase().contains(kw)) {
                    continue;
                }
                ConfigurationSection rw = tier.getConfigurationSection("rewards");
                TierListRowVO row = new TierListRowVO();
                row.setKey(key);
                row.setPermission(tier.getString("permission", ""));
                row.setEnabled(tier.getBoolean("enabled", true));
                row.setMoney(rw == null ? 0 : rw.getInt("money", 0));
                row.setPoints(rw == null ? 0 : rw.getInt("points", 0));
                row.setCommandsCount(rw == null ? 0 : rw.getStringList("commands").size());
                row.setItemsCount(rw == null ? 0 : ItemSerializer.readMaps(rw, "items").size());
                int claimed = 0;
                for (List<String> ts : claimedMonth.values()) {
                    if (ts.contains(key)) {
                        claimed++;
                    }
                }
                row.setClaimedThisMonth(claimed);
                rows.add(row);
            }
        }
        return PageUtils.page(rows, pageNum, pageSize);
    }

    @Override
    public AjaxResult detail(String tierKey) {
        ConfigurationSection tier = McRewards.tier(plugin, tierKey);
        if (tier == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        ConfigurationSection rw = tier.getConfigurationSection("rewards");
        TierDetailVO vo = new TierDetailVO();
        vo.setKey(tierKey);
        vo.setPermission(tier.getString("permission", ""));
        vo.setEnabled(tier.getBoolean("enabled", true));
        RewardsVO rewards = new RewardsVO();
        rewards.setMoney(rw == null ? 0 : rw.getInt("money", 0));
        rewards.setPoints(rw == null ? 0 : rw.getInt("points", 0));
        rewards.setCommands(rw == null ? Collections.<String>emptyList() : rw.getStringList("commands"));
        rewards.setItems(rw == null ? Collections.<Map<String, Object>>emptyList()
                : ItemSerializer.readMaps(rw, "items"));
        vo.setRewards(rewards);
        return AjaxResult.success(vo);
    }

    @Override
    public AjaxResult save(String tierKey, McTierSaveVO body) {
        ConfigurationSection existing = McRewards.tier(plugin, tierKey);
        if (existing == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        if (body == null) {
            return AjaxResult.error("请求体为空");
        }
        try {
            YamlConfiguration rewards = McRewards.yaml(plugin);
            String base = "tiers." + tierKey;
            if (body.getPermission() != null) {
                rewards.set(base + ".permission", body.getPermission());
            }
            if (body.getEnabled() != null) {
                rewards.set(base + ".enabled", body.getEnabled());
            }
            if (body.getMoney() != null) {
                rewards.set(base + ".rewards.money", body.getMoney());
            }
            if (body.getPoints() != null) {
                rewards.set(base + ".rewards.points", body.getPoints());
            }
            if (body.getCommands() != null) {
                rewards.set(base + ".rewards.commands", body.getCommands());
            }
            if (body.getItems() != null) {
                rewards.set(base + ".rewards.items", body.getItems());
            }
            McRewards.save(plugin, rewards);
            return ok("已保存档位 " + tierKey);
        } catch (Exception e) {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }

    @Override
    public AjaxResult toggle(String tierKey) {
        ConfigurationSection tier = McRewards.tier(plugin, tierKey);
        if (tier == null) {
            return AjaxResult.notFound("档位不存在: " + tierKey);
        }
        boolean now = !tier.getBoolean("enabled", true);
        try {
            YamlConfiguration rewards = McRewards.yaml(plugin);
            rewards.set("tiers." + tierKey + ".enabled", now);
            McRewards.save(plugin, rewards);
        } catch (Exception e) {
            return AjaxResult.error("切换失败: " + e.getMessage());
        }
        TierToggleVO vo = new TierToggleVO();
        vo.setEnabled(now);
        return AjaxResult.success((now ? "已启用档位 " : "已禁用档位 ") + tierKey, vo);
    }

    /** 仅消息的成功响应（{@code msg} 携带提示，{@code data} 留空）。 */
    private static AjaxResult ok(String msg) {
        return new AjaxResult(AjaxResult.SUCCESS, msg);
    }
}
