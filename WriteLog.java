package pushMessage_MultiThread_DBCP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WriteLog {
	private String fileName;
	private int columnCount;
	private String saveTime;

	public WriteLog(int columnCount, String saveTime) {
		this.columnCount = columnCount;
		this.saveTime = saveTime;
		this.fileName = makeFileName();
	}

// 로그파일에 전송 기록을 저장하는메소드 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void writeSendMsgLog(String msg) {

		File file = new File(fileName);
		try {
			FileWriter fw = new FileWriter(file, true);
			fw.write(msg);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

// 파일 이름 만들어 반환하는 메소드 ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String makeFileName() {
		String fileName;

		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		fileName = "C:/msgLog/" + date + "_" + saveTime + "_" + columnCount + ".log";

		return fileName;
	}
}
