package soys.soysmonthlycard.spring.entity.vo;

import java.util.List;

/** 审计日志视图 VO（{@code GET /admin/logs}）。 */
public class LogViewVO {
    private boolean enabled;
    private String file;
    private List<String> lines;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<String> getLines() {
        return lines;
    }

    public void setLines(List<String> lines) {
        this.lines = lines;
    }
}
