package pushMessage_MultiThread_DBCP;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AgentPushMessage {

	private JFrame frame;
	private ScheduledExecutorService g_service; // 스레드 풀로 2개 이상의 스레드를 실행시킬때 사용
	private ExecutorService g_executorService;
	private int g_sendingMsgRange; // 한번에 전송하고자 하는 푸시 메세지 수
	private WriteLog wl;
	private Time t;
	private ConnectionPoolManager cpm;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					AgentPushMessage window = new AgentPushMessage();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public AgentPushMessage() {
		g_sendingMsgRange = 15; // 한번에 메세지를 보낼때 몇개씩 보낼지 설정
		g_executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); // 내 컴퓨터의 코어 수 만큼
		t = new Time();
		cpm = new ConnectionPoolManager();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		JButton btnPauseMsg = new JButton("중지");
		btnPauseMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				g_service.shutdownNow();
			}
		});
		btnPauseMsg.setFont(new Font("맑은 고딕", Font.BOLD, 15));
		btnPauseMsg.setBounds(65, 147, 119, 39);
		frame.getContentPane().add(btnPauseMsg);

		JButton btnSendMsg = new JButton("전송");
		btnSendMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Thread g_th = new Thread(new Runnable() { // Runnable을 익명함수로 사용, 쓰레드 생성

					@Override
					public void run() { // 실행할 프로세스 작성
						// TODO Auto-generated method stub
						try {
							getReceiverInfoFromDB();

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});

				g_service = Executors.newSingleThreadScheduledExecutor(); // 스레드 특정 주기마다 수행하고 싶을때 생성 후
				// scheduleAtFixedRate() 메소드 사용 scheduleAtFixedRate(실행가능객체, 지연시간, 주기, 단위)를 사용한다.
				g_service.scheduleAtFixedRate(g_th, 0, 3, TimeUnit.SECONDS);
				// 시작딜레이(initialDelay) 이후 첫번째 실행을 시작으로 지정한 시간(period)만큼 차이로 정확하게 반복 실행 한다.

			}

		});
		btnSendMsg.setFont(new Font("맑은 고딕", Font.BOLD, 15));
		btnSendMsg.setBounds(239, 147, 119, 39);
		frame.getContentPane().add(btnSendMsg);
	}

	public void getReceiverInfoFromDB() throws SQLException, IOException, Exception {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		ReceiveInfo ri = null;
		
		System.out.println("==================== getReceiverInfoFromDB()메소드 실행 ====================");
		
		ArrayList<ReceiveInfo> totalReceiveInfoList = new ArrayList<ReceiveInfo>(); // 전체 받는사람의 정보가 들어있는 List
		String msgContent = null;
		String blockMsgId = null;
		String saveTime = null;

		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			// MIN(msgno)한 이유는 Client에서 메세지 전송을 먼저 요청한 순서대로 보내주기 위해서 이다.
			String sql = "select * from tb_receiver_info where msgno = (select MIN(msgno) from tb_receiver_info where tran_status = 0)";
			rs = stmt.executeQuery(sql);

			if (!(rs.isBeforeFirst())) { // DB에 보내고자 하는 메세지가 없는 경우
				System.out.println(" =============== 보낼 메세지가 없습니다. ===============");
			} else { // DB에 보내고자 하는 메세지가 있는 경우
				while (rs.next()) {
					blockMsgId = rs.getString("msgno");
					String phoneNum = rs.getString("phoneNum");
					String pushToken = rs.getString("pushToken");
					String phoneType = rs.getString("phoneType");
					msgContent = rs.getString("msgContent");
					ri = new ReceiveInfo(phoneNum, pushToken, phoneType);
					totalReceiveInfoList.add(ri);
				}
				;

				// 메세지를 전송시 시간을 saveTime에 저장
				saveTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

				wl = new WriteLog(totalReceiveInfoList.size(), saveTime);

				if (totalReceiveInfoList.size() == 1) { // 조회 결과 단문일때
//					singleSendPushNotification(blockMsgId, totalReceiveInfoList, msgContent);
				} else { // 조회 결과 복문일때
						multiSendPushNotification(blockMsgId, totalReceiveInfoList, msgContent);
				}
			}
		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			JOptionPane.showMessageDialog(null, "DB 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // DB연결실패시 팝업창 뜨게함
		} finally { // 자원 반납
			if (rs != null)
				try {
					rs.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
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

	// 복문 푸시 메세지 보내는 메소드
	public void multiSendPushNotification(String blockMsgId, ArrayList<ReceiveInfo> totalReceiveInfoList, String msg)
			throws Exception {
		List<ReceiveInfo> iosList = new ArrayList<ReceiveInfo>(); // ios타입의 전송될 사람의 정보를 저장하는 ArrayList
		List<ReceiveInfo> androidList = new ArrayList<ReceiveInfo>(); // android타입의 전송될 사람의 정보를 저장하는 ArrayList
		List<String> sendIosNumTotalList = new ArrayList<String>(); // 전송될 아이폰 타입의 핸드폰 번호를 저장하는 ArrayList
		List<String> sendAndroidNumTotalList = new ArrayList<String>(); // 전송될 안드로이드 타입의 핸드폰 번호를 저장하는 ArrayList
		HashMap<Integer, List<ReceiveInfo>> groupAndroid = new HashMap<>(); // 그룹별로 나눈 ReceiveInfo 리스트를 HashMap 값으로
																			// 넣어준다.
		HashMap<Integer, List<ReceiveInfo>> groupIos = new HashMap<>();

		// 받는사람의 목록에서 ios와 android를 나누는 메소드
		dividePhoneType(totalReceiveInfoList, iosList, androidList, sendIosNumTotalList, sendAndroidNumTotalList);

		String decodeMsg = URLDecoder.decode(msg, "UTF-8"); // 메세지 디코더 작업

		// 로그 Date, Time, Total 기록하기
		wl.writeSendMsgLog("Date: " + t.getNowDate() + "\n");
		wl.writeSendMsgLog("Time: " + t.getNowTime() + "\n");
		wl.writeSendMsgLog("Total: " + totalReceiveInfoList.size() + "\n");

		// android 총 개수 로그에 저장
		wl.writeSendMsgLog("android: " + sendAndroidNumTotalList.size() + "\n");// 안드로이드 개수를 로그 기록

		// android푸시 메시지 전송
		int androidGroupSize = getGroupSize(androidList); // 전체 받는 사람을 그룹화 되면 몇등분 되는지 사이즈 구하기
		setGroupRange(androidList, groupAndroid, androidGroupSize); // android인 사람들 15명씩 그룹화 하여 HashMap에 저장

		MultiThread[] androidMt = new MultiThread[androidGroupSize];
		int androidIndex = 0;
		for (Entry<Integer, List<ReceiveInfo>> entry : groupAndroid.entrySet()) {
			androidMt[androidIndex++] = new MultiThread(entry.getValue(), decodeMsg, blockMsgId, "android", wl, cpm);

		}
		// ios 총 개수 로그에 저장
		wl.writeSendMsgLog("ios: " + sendIosNumTotalList.size() + "\n");

		// ios푸시 메시지 전송
		int iosGroupSize = getGroupSize(iosList);
		setGroupRange(iosList, groupIos, iosGroupSize);

		MultiThread[] iosMt = new MultiThread[iosGroupSize];
		int iosIndex = 0;
		for (Entry<Integer, List<ReceiveInfo>> entry : groupIos.entrySet()) {
			iosMt[iosIndex++] = new MultiThread(entry.getValue(), decodeMsg, blockMsgId, "ios", wl, cpm);
		}

		for (int i = 0; i < androidIndex; i++) {
			androidMt[i].start();
		}

		for (int i = 0; i < iosIndex; i++) {
			iosMt[i].start();
		}

	}

	// android인 사람들 15명씩 그룹화 하여 HashMap에 저장
	public void setGroupRange(List<ReceiveInfo> osList, HashMap<Integer, List<ReceiveInfo>> groupOs, int groupSize) {
		int start = 0;
		int range = 0;

		for (int i = 0; i < groupSize; i++) {
			range = Math.min(osList.size() - start, g_sendingMsgRange); // 슬라이스 범위 설정

			ArrayList<ReceiveInfo> groupReceiver = new ArrayList<ReceiveInfo>();

			for (int j = start; j < start + range; j++) {
				groupReceiver.add(osList.get(j));
			}
			groupOs.put(i, groupReceiver);

			start += range;
		}

	}

	// android인 받는 사람들을 특정 range값으로 그룹한 사이즈 구하기
	public int getGroupSize(List<ReceiveInfo> sendPhoneList) {
		int groupSize = sendPhoneList.size() / g_sendingMsgRange;
		if ((sendPhoneList.size() % g_sendingMsgRange) > 0) {
			groupSize += 1;
		}

		return groupSize;
	}

	// 받는사람의 목록에서 ios와 android를 나누는 메소드
	public void dividePhoneType(ArrayList<ReceiveInfo> totalReceiveInfoList, List<ReceiveInfo> iosList,
			List<ReceiveInfo> androidList, List<String> sendIosNumTotalList, List<String> sendAndroidNumTotalList) {
		ReceiveInfo receiveInfo = null;

		for (int i = 0; i < totalReceiveInfoList.size(); i++) { // mapkey는 hashmap의 핸드폰번호이다.
			receiveInfo = totalReceiveInfoList.get(i); // receiveList.get(mapkey)로 mapkey을 키값 넣어주면 value값 receiveInfo가
														// 나온다.
			if (receiveInfo.getPhoneType().equals("ios")) {
				sendIosNumTotalList.add(receiveInfo.getPhoneNum()); // 전송되는 핸드폰 번호 리스트로 저장 ==> Log기록시 필요함
				iosList.add(receiveInfo); // ios를 갖는 ReceiveInfo를 리스트로 저장
			} else {
				sendAndroidNumTotalList.add(receiveInfo.getPhoneNum());
				androidList.add(receiveInfo);
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