package pushMessage_MultiThread_DBCP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MultiThread extends Thread {
	private List<ReceiveInfo> groupOS;
	private String decodeMsg;
	private String blockMsgId;
	private String OSType;
	private WriteLog wl;
	private Time t;
	private ConnectionPoolManager cpm;

	public static Object Lock1 = new Object();
	public static Object Lock2 = new Object();

	public MultiThread(List<ReceiveInfo> groupOS, String decodeMsg, String blockMsgId, String OSType, WriteLog wl, ConnectionPoolManager cpm) {
		this.groupOS = groupOS;
		this.decodeMsg = decodeMsg;
		this.blockMsgId = blockMsgId;
		this.OSType = OSType;
		this.wl = wl;
		this.t = new Time();
		this.cpm = cpm;
	}

	@Override
	public void run() {
		try {
			JSONObject json = insertJsonObjcetValue(groupOS, decodeMsg, OSType);

			List<String> sendNumList = new ArrayList<String>();
			for (int i = 0; i < groupOS.size(); i++) {
				sendNumList.add(groupOS.get(i).getPhoneNum());
			}

			updateDateSent(blockMsgId, sendNumList); // DB에 보내는 시간 date_sent 속성값 update

			String result = "";

			// 로그메세지 기록때문에 synchroinized를 설정함.
			synchronized (Lock1) {
				result = multiSend(blockMsgId, json, sendNumList, OSType);
			}
			
			insertResultMsg(blockMsgId, result, sendNumList); // DB에 전송결과와 date_result 값을 update
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	public String multiSend(String blockMsgId, JSONObject sendInfo, List<String> sendNumList, String phoneType)
			throws Exception {
		String AUTH_KEY_FCM = "AAAAD34pYd4:APA91bHXVN9KedI4WLxu2MdJUdGlnTufzws0mC6x-8h0rFcgepzBD-VkcOW9G6nRle-o2o4-U7GwI-bQMWRTuGtgqjW5Mrxpf4y87WrmYsNnJ5ZcvV2JeGYB9HpcFgcD_RMdQYWtM9_b";
		String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
		URL url = new URL(API_URL_FCM);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
		conn.setRequestProperty("Content-Type", "application/json");

		OutputStreamWriter wr = null;
		BufferedReader br = null;

		try {
			wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(sendInfo.toString());
			wr.flush();

			br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Http 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // Http연결실패시 팝업창 뜨게함
		}

		// Phone 로그 기록 (받는사람의 핸드폰 번호를 기록)
		wl.writeSendMsgLog("\n[" + phoneType + "] Phone: \n"); // 안드로이드 핸드폰 번호 로그 기록

		for (int i = 0; i < sendNumList.size(); i++) {
			wl.writeSendMsgLog(sendNumList.get(i) + "\t");
		}

		// 전송 메세지 로그 기록
		wl.writeSendMsgLog("\nMessage: " + sendInfo.toJSONString() + "\n");

		String output;

		System.out.println("\nOutput from Server .... ");
		System.out.println("Phone Type: " + phoneType);

		String resultMsg = null;

		while ((output = br.readLine()) != null) {
			resultMsg = output;
			// Result 로그 기록
			wl.writeSendMsgLog("Result: \n" + output + "\n");
		}

		conn.disconnect();

		return resultMsg;

	}

	// 15명씩 그룹화 된 사람들을 JSONObject에 값 넣어주는 메소드
	public JSONObject insertJsonObjcetValue(List<ReceiveInfo> groupOS, String decodeMsg, String OSType) {
		JSONObject json = null;
		JSONArray tokens = null;
		
		// android type일때 JSONObject 값 할당
		if (OSType.equals("android")) {
			tokens = new JSONArray();

			for (int i = 0; i < groupOS.size(); i++) {
				tokens.add(groupOS.get(i).getToken());
			}

			json = new JSONObject();
			json.put("registration_ids", tokens);
			json.put("delay_while_idle", true);
			json.put("collapse_key", getMsgId());
			json.put("time_to_live", 259200);
			json.put("content_available", true);

			JSONObject dataInfo = new JSONObject();
			dataInfo.put("title", decodeMsg.substring(0, Math.min(11, decodeMsg.length()))); // Notification title
			dataInfo.put("body", decodeMsg); // Notification
			dataInfo.put("memberid", "E0006K0001");
			dataInfo.put("senddate", t.getNowDateTime());
			dataInfo.put("msgtype", "NEWS");
			dataInfo.put("page", "a.2.1.asp");

			json.put("data", dataInfo);
		}
		// ios type일때 JSONObject 값 할당
		else {
			tokens = new JSONArray();

			for (int i = 0; i < groupOS.size(); i++) {
				tokens.add(groupOS.get(i).getToken());
			}

			json = new JSONObject();

			json.put("registration_ids", tokens);
			json.put("delay_while_idle", true);
			json.put("collapse_key", getMsgId());
			json.put("time_to_live", 259200);
			json.put("content_available", true);
			json.put("priority", "high");
			json.put("mutable_content", true);

			JSONObject notifyInfo = new JSONObject();
			notifyInfo.put("title", decodeMsg.substring(0, Math.min(11, decodeMsg.length()))); // Notification title
			notifyInfo.put("body", decodeMsg); // Notification
			notifyInfo.put("sound", "default");
			notifyInfo.put("badge", "1");

			json.put("notification", notifyInfo);

			JSONObject dataInfo = new JSONObject();
			dataInfo.put("memberid", "E0006K0001");
			dataInfo.put("senddate", t.getNowDateTime());
			dataInfo.put("msgtype", "NEWS");
			dataInfo.put("page", "a.2.1.asp");

			json.put("data", dataInfo);
		}

		return json;
	}

	public void insertResultMsg(String blockMsgId, String resultMsg, List<String> sendNumList) throws ParseException, SQLException {
		// 결과 메세지 JSON형식으로 parse해서 결과별로 잘라서 변수에 넣어주기
		JSONParser jParser = new JSONParser();
		JSONObject jObject = (JSONObject) jParser.parse(resultMsg);

		JSONArray jResultArray = (JSONArray) jObject.get("results");
		ArrayList<String> msgIdList = new ArrayList<String>();

		if (jObject.get("success").toString().equals("0")) { // 전송 실패시
			for (int i = 0; i < jResultArray.size(); i++) {
				JSONObject jResult = (JSONObject) jResultArray.get(i);
				msgIdList.add(jResult.get("error").toString());
			}
			System.out.println("Multi Notification is sent failed");
			synchronized (Lock1) {
				updateDateResult(3, jObject.get("multicast_id").toString(), jObject.get("canonical_ids").toString(),
						msgIdList, blockMsgId, sendNumList, getMsgId());
			}
		} else { // 전송 성공시
			for (int i = 0; i < jResultArray.size(); i++) {
				JSONObject jResult = (JSONObject) jResultArray.get(i);
				msgIdList.add(jResult.get("message_id").toString());
			}
			System.out.println("Multi Notification is sent successfully");
			synchronized (Lock2) {
				updateDateResult(2, jObject.get("multicast_id").toString(), jObject.get("canonical_ids").toString(),
						msgIdList, blockMsgId, sendNumList, getMsgId());
			}
		}
	}

	public void updateDateResult(int tran, String mul, String can, ArrayList<String> msgIdList, String blockMsgId,
			List<String> phoneNumList, String collapse_key) throws SQLException {
		Connection conn = null;
		Statement stmt = null;

		int tran_status = tran;
		String multicast_id = mul;
		String canonical_ids = can;
		String message_id = null;
		String sql = null;
		String phoneNum = "";

		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			for (int i = 0; i < phoneNumList.size(); i++) {
				phoneNum += "'" + phoneNumList.get(i) + "'";
				if (i < phoneNumList.size() - 1) {
					phoneNum += ", ";
				}
			}

			sql = "update tb_receiver_info set date_result = GETDATE(), tran_status = " + tran_status
					+ ", multicast_id = '" + multicast_id + "', canonical_ids = '" + canonical_ids + "', message_id = '"
					+ message_id + "', collapse_key = '" + collapse_key + "' where msgno = '" + blockMsgId
					+ "' and phoneNum in (" + phoneNum + ");";

			stmt.executeUpdate(sql);

		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			JOptionPane.showMessageDialog(null, "DB 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // DB연결실패시 팝업창 뜨게함
		}

		finally { // 자원 반납
			if (stmt != null)
				try {
					stmt.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			if (conn != null)
				try {
					conn.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
		}
	}

	// 전송중일때 DB의 date_sent 속성값을 update 해주는 메소드
	public void updateDateSent(String blockMsgId, List<String> phoneNumList)
			throws SQLException, IOException, Exception {
		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		String phoneNum = "";

		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			for (int i = 0; i < phoneNumList.size(); i++) {
				phoneNum += "'" + phoneNumList.get(i) + "'";
				if (i < phoneNumList.size() - 1) {
					phoneNum += ", ";
				}
			}

			sql = "update tb_receiver_info set date_sent = GETDATE(), tran_status = 1 where msgno = '" + blockMsgId
					+ "' and phoneNum in (" + phoneNum + ");";
			stmt.executeUpdate(sql);
		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			JOptionPane.showMessageDialog(null, "DB 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // DB연결실패시 팝업창 뜨게함
		} finally { // 자원 반납
			if (stmt != null)
				try {
					stmt.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			if (conn != null)
				try {
					conn.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
		}

	}

	// 메세지 일련번호 만드는 메소드
	public String getMsgId() {
		String id = "";
		int cnt = 1;

		id = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		id += String.format("%05d", cnt++);

		return id;
	}

}
