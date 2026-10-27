package cc.ruixing.framework.tarn.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC数据行映射接口
 */
public interface RowMapper {
    public Object mapRow(ResultSet rs) throws SQLException;
}
