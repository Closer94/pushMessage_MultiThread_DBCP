package pushMessage_MultiThread_DBCP;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Time {
	
		// 현재날짜와 시간을 불러오는 메소드
		public String getNowDateTime() {

			return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		}

		// 현재날짜를 불러오는 메소드
		public String getNowDate() {

			return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
		}

		//현재시간을 불러오는 메소드
		public String getNowTime() {

			return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
		}
}
