package pushMessage_MultiThread_DBCP;

import java.sql.*;

import javax.sql.DataSource;

//BasicDataSource을 사용하기 위해 org.apache.commons.dbcp.BasicDataSource을 import 시켜준다.
import org.apache.commons.dbcp2.BasicDataSource;

public class ConnectionPoolManager {

	private BasicDataSource ds = null;

// DBCP를 생성한다.

	private void setupDataSource() {
		
		if (ds == null) {

			ds = new BasicDataSource();

			String url = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare";
//			String url = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
			
			String className = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

			String username = "kabsung3";

			String password = "hyung269";

			ds.setDriverClassName(className); // 2

			ds.setUrl(url); // 3

			ds.setUsername(username); // 4

			ds.setPassword(password);

//			ds.setMaxActive(10); default값으로 대체 8

			ds.setInitialSize(10);

			ds.setMinIdle(5);

			ds.setMaxWaitMillis(5000);

			ds.setPoolPreparedStatements(true); // 5

		}

	}

// BasicDataSource로부터 connection을 얻어온다.

	public Connection getConnection() throws SQLException {

		setupDataSource();

		return ds.getConnection(); // 6
	}
}