package examples.orm.entity;

import com.dfrdev.tarn.aop.annotation.Ekey;
import com.dfrdev.tarn.aop.annotation.Etable;

import java.util.List;
import java.util.Map;

/**
 * ORM 使用示例实体：笔记（演示复杂字段 JSON 序列化）。
 * <p>
 * Tarn ORM 对复杂类型字段的处理规则（入库自动 JSON 序列化、查询自动反序列化）：
 * <ul>
 *     <li>{@code List} / 数组 → JSON 数组字符串</li>
 *     <li>{@code Map} / {@code Set} → JSON 对象字符串</li>
 *     <li>自定义类（非 java.** 开头）→ JSON 对象字符串</li>
 * </ul>
 * 数据库列只需 VARCHAR/TEXT 即可承载，无需 JSON 列类型。
 *
 * <pre>
 * CREATE TABLE demo_note (
 *     ID    BIGINT PRIMARY KEY AUTO_INCREMENT,
 *     TITLE VARCHAR(128),
 *     TAGS  VARCHAR(512),   -- List&lt;String&gt; 序列化: ["orm","tarn"]
 *     META  VARCHAR(512),   -- Map 序列化: {"creator":"dfr"}
 *     ATTRS VARCHAR(512)    -- 自定义类序列化: {"pinned":true,"level":3}
 * );
 * </pre>
 *
 * @author dfr
 * @since 2.1
 */
@Etable("demo_note")
public class DemoNote {

    @Ekey(key = "id", isAutoIncrement = true)
    private Long id;

    private String title;

    /** List 字段：入库自动 JSON 序列化 */
    private List<String> tags;

    /** Map 字段：入库自动 JSON 序列化 */
    private Map<String, Object> meta;

    /** 自定义类字段：入库自动 JSON 序列化 */
    private Attrs attrs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public Attrs getAttrs() {
        return attrs;
    }

    public void setAttrs(Attrs attrs) {
        this.attrs = attrs;
    }

    @Override
    public String toString() {
        return "DemoNote{id=" + id + ", title='" + title + "', tags=" + tags
                + ", meta=" + meta + ", attrs=" + attrs + "}";
    }

    /**
     * 自定义嵌套类字段示例
     */
    public static class Attrs {

        private Boolean pinned;
        private Integer level;

        public Boolean getPinned() {
            return pinned;
        }

        public void setPinned(Boolean pinned) {
            this.pinned = pinned;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        @Override
        public String toString() {
            return "Attrs{pinned=" + pinned + ", level=" + level + "}";
        }
    }
}
