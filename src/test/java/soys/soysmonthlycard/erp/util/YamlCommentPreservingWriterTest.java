package soys.soysmonthlycard.erp.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 保留注释写入器测试 —— 这是「配置编辑抹注释」修复的核心，
 * 曾出现两个 bug（缩进栈越界、标量被写成带引号字符串），固化回归保护。
 */
class YamlCommentPreservingWriterTest {

    @Test
    void preservesCommentsAndTypes(@TempDir File dir) throws Exception {
        File f = new File(dir, "config.yml");
        String original = String.join("\n",
                "# 主配置",
                "settings:",
                "  # 每月几号",
                "  claim-day: 1 # 默认 1 号",
                "  debug: false",
                "  units:",
                "    money: \"金币\"",
                "") ;
        Files.write(f.toPath(), original.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> units = new LinkedHashMap<>();
        units.put("money", "元宝");
        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("claim-day", 15);   // Integer 必须写成裸数字
        settings.put("debug", true);      // Boolean 必须写成裸 true
        settings.put("units", units);     // String 必须带引号
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("settings", settings);

        int written = YamlCommentPreservingWriter.savePreservingComments(f, data);
        assertEquals(3, written);

        String out = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue(out.contains("# 主配置"), "文件头注释应保留");
        assertTrue(out.contains("# 每月几号"), "节注释应保留");
        assertTrue(out.contains("claim-day: 15 # 默认 1 号"), "值替换且行内注释保留");
        assertTrue(out.contains("debug: true"), "布尔应为裸值");
        assertFalse(out.contains("\"true\""), "布尔不得带引号");
        assertFalse(out.contains("\"15\""), "数字不得带引号");
        assertTrue(out.contains("money: \"元宝\""), "字符串应带引号");
    }

    @Test
    void missingKeysKeepOriginal(@TempDir File dir) throws Exception {
        File f = new File(dir, "config.yml");
        Files.write(f.toPath(), ("settings:\n  claim-day: 1\n  debug: false\n")
                .getBytes(StandardCharsets.UTF_8));

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("claim-day", 9); // data 里没有 debug
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("settings", settings);

        int written = YamlCommentPreservingWriter.savePreservingComments(f, data);
        assertEquals(1, written);
        String out = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        assertTrue(out.contains("debug: false"), "data 缺失的键应保持原值");
    }

    @Test
    void createsBackup(@TempDir File dir) throws Exception {
        File f = new File(dir, "config.yml");
        Files.write(f.toPath(), "a: 1\n".getBytes(StandardCharsets.UTF_8));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("a", 2);

        YamlCommentPreservingWriter.savePreservingComments(f, data);
        File[] baks = dir.listFiles((d, n) -> n.startsWith("config.yml.bak_"));
        assertNotNull(baks);
        assertEquals(1, baks.length, "保存前应生成一份 .bak 备份");
    }
}
