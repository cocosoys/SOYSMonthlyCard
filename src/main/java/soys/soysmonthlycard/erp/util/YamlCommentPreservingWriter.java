package soys.soysmonthlycard.erp.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 保留注释的 YAML「逐键写回」工具。
 *
 * <p>背景：Bukkit {@code YamlConfiguration#save} 会把整份配置重新序列化，
 * 所有注释与空行全部丢失。ERP「插件配置」页保存 config.yml 时，
 * 原文件 3KB 中约 36 行是中文注释说明——全量重写一次就退化成裸键值，
 * 排障依据全无。</p>
 *
 * <p>策略：把原文件按行读入，只替换「叶子键所在行」的 value 部分，
 * 注释行、空行、键的顺序、行内注释一概保留：</p>
 * <pre>
 *   settings:
 *     # 每月几号(含)之后可以领取月卡礼包，范围建议 1-28
 *     claim-day: 1     ← 只把 "1" 换成新值，"#" 之前的注释与缩进不动
 * </pre>
 *
 * <p>边界：</p>
 * <ul>
 *   <li>只更新「原文件已存在」的键；Map 里多出来的键不追加，避免臆造结构。</li>
 *   <li>Map 里缺失的键保持原值不动，避免误删未知配置。</li>
 *   <li>层级按缩进推导（每级 2 空格，兼容 4 空格），用「缩进栈」还原 dotted path。</li>
 *   <li>只处理标量行（{@code key: value}）；含嵌套子键的父行（{@code key:} 后为空）不写值。</li>
 * </ul>
 */
public final class YamlCommentPreservingWriter {

    private static final Pattern KEY_LINE = Pattern.compile(
            "^(\\s*)([A-Za-z0-9_.\\-]+)(\\s*:)(.*)$");

    private YamlCommentPreservingWriter() {
    }

    /**
     * 按 map 中的值更新 file，保留注释与结构。保存前生成 {@code .bak_yyyyMMdd-HHmmss} 备份。
     *
     * @return 实际写入的叶子键数量
     */
    public static int savePreservingComments(File file, Map<String, Object> data) throws IOException {
        if (file == null || data == null) {
            throw new IllegalArgumentException("file/data 不可为 null");
        }
        if (!file.isFile()) {
            throw new IOException("配置文件不存在: " + file.getPath());
        }

        backup(file);

        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        Map<String, Object> flat = new LinkedHashMap<>();
        flatten(data, "", flat);

        // 缩进栈：stack.get(indent) = 该缩进层的键名。用 TreeMap 思路按缩进深度维护。
        List<String> pathStack = new ArrayList<>();
        int written = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = KEY_LINE.matcher(line);
            if (!m.matches()) {
                continue; // 注释行 / 空行 / 非键值行：原样保留
            }
            String indent = m.group(1);
            String key = m.group(2);
            String colon = m.group(3);
            String rest = m.group(4);

            int depth = indentDepth(indent);
            // 回退到本行所在的层级：删除比当前更深的层级
            while (pathStack.size() > depth + 1) {
                pathStack.remove(pathStack.size() - 1);
            }
            // 确保有 depth+1 个槽位（容忍跳级缩进，中间补空串）
            while (pathStack.size() < depth + 1) {
                pathStack.add("");
            }
            pathStack.set(depth, key);

            StringBuilder path = new StringBuilder();
            for (int d = 0; d <= depth && d < pathStack.size(); d++) {
                String seg = pathStack.get(d);
                if (seg == null || seg.isEmpty()) {
                    continue;
                }
                if (path.length() > 0) {
                    path.append('.');
                }
                path.append(seg);
            }
            String dotted = path.toString();

            if (!flat.containsKey(dotted)) {
                continue; // map 未涉及此键：保持原样
            }
            String tail = rest.trim();
            // 父节点行（key: 后无值或已是注释）不写，避免把 map 结构覆成标量
            if (tail.isEmpty() || tail.startsWith("#")) {
                continue;
            }

            // 拆出「值 + 行内注释」：行内注释以 " #" 开头（且 # 前是空白）
            String inlineComment = extractInlineComment(rest);
            String newValue = scalar(flat.get(dotted));
            lines.set(i, indent + key + colon + " " + newValue + inlineComment);
            written++;
        }

        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        return written;
    }

    /** 生成 .bak_yyyyMMdd-HHmmss 备份（同目录）。 */
    private static void backup(File file) throws IOException {
        String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        File bak = new File(file.getParentFile(), file.getName() + ".bak_" + ts);
        Files.copy(file.toPath(), bak.toPath());
    }

    /** 把嵌套 Map 展平为 dotted-path → 原始值（只保留叶子），保留值的原始类型。 */
    private static void flatten(Map<String, Object> map, String prefix, Map<String, Object> out) {
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            if (key == null) {
                continue;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object v = e.getValue();
            if (v instanceof Map) {
                flatten((Map<String, Object>) v, path, out);
            } else {
                out.put(path, v);
            }
        }
    }

    /** 标量序列化为 YAML 行内值。 */
    private static String scalar(Object v) {
        if (v == null) {
            return "''";
        }
        if (v instanceof Boolean || v instanceof Number) {
            return String.valueOf(v);
        }
        if (v instanceof List) {
            List<?> list = (List<?>) v;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(scalar(list.get(i)));
            }
            return sb.append(']').toString();
        }
        // 字符串：统一双引号并转义反斜杠与双引号
        String s = String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + s + "\"";
    }

    /** 从冒号后的余串中拆出行内注释（" #..."），返回带前导空格的注释部分。 */
    private static String extractInlineComment(String rest) {
        int idx = -1;
        for (int i = 0; i < rest.length(); i++) {
            if (rest.charAt(i) == '#' && (i == 0 || Character.isWhitespace(rest.charAt(i - 1)))) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return "";
        }
        String prefix = rest.substring(0, idx);
        // 保留注释前原有的空白
        int trailing = prefix.length() - prefix.replaceAll("\\s+$", "").length();
        return prefix.substring(prefix.length() - trailing) + rest.substring(idx);
    }

    /** 缩进深度：每 2 空格一级（4 空格算 2 级），至少 0。 */
    private static int indentDepth(String indent) {
        int spaces = 0;
        for (int i = 0; i < indent.length(); i++) {
            char c = indent.charAt(i);
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                spaces += 2;
            }
        }
        return spaces / 2;
    }
}
