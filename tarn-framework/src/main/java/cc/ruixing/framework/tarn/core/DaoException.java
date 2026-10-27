package cc.ruixing.framework.tarn.core;

import java.sql.SQLException;


public class DaoException extends Throwable {
    public DaoException(String message, SQLException e) {
      super(message,e);
    }
}
