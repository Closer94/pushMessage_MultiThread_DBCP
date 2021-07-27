package pushMessage_MultiThread_DBCP;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;

public class ClientPushMessage {

	private JFrame frmSinglemessagesend;
	private JTextField inputPhoneNumBox;
	DefaultListModel resultMessageModel; // 결과메세지의 리스트에 값을 추가하기 위한 변수
	DefaultListModel receiveListModel; // 받는 사람의 리스트에 값을 추가하기 위한 변수
	JList resultMsgList;
	JCheckBox chkShowMsg;
	JCheckBox chkSaveMsg;
	HashMap<String, ReceiveInfo> receiveList;
	String saveTime;
	ConnectionPoolManager cpm;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ClientPushMessage window = new ClientPushMessage();
					window.frmSinglemessagesend.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ClientPushMessage() {
		receiveList = new HashMap<String, ReceiveInfo>();
		cpm = new ConnectionPoolManager();
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSinglemessagesend = new JFrame();
		frmSinglemessagesend.setTitle("SingleMessageSend");
		frmSinglemessagesend.setBounds(100, 100, 725, 620);
		frmSinglemessagesend.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmSinglemessagesend.getContentPane().setLayout(null);

		// 스크롤바 추가
		// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		JScrollPane StateMessageScrollPane = new JScrollPane();
		StateMessageScrollPane.setBounds(27, 370, 663, 161);
		frmSinglemessagesend.getContentPane().add(StateMessageScrollPane);

		// 결과메세지 나오는 List에 값
		// 추가///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		resultMessageModel = new DefaultListModel();
		resultMsgList = new JList(resultMessageModel); // resultMessageModel을 JList 생성시 매개변수로 넣어준다.
		StateMessageScrollPane.setViewportView(resultMsgList); // 스크롤바에 JList를 추가해준다.

		// 핸드폰 번호 입력하는
		// inputBox////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		inputPhoneNumBox = new JTextField();

		inputPhoneNumBox.addFocusListener(new FocusListener() {
			// inputPhoneNum을 input하는 곳에서 포커스 아웃 될때
			// //////////////////////////////////////////////////////////////////////////////////////////
			@Override
			public void focusLost(FocusEvent e) {
				String phoneNum = inputPhoneNumBox.getText();
				String result = null;

				result = isValidPhoneNum(phoneNum);

				if (result == null) {
					inputPhoneNumBox.setText("수신번호");
				} else {
					inputPhoneNumBox.setText(result);
				}

			}

			// inputPhoneNum을 input하는 곳에서 포커스를 둘때 핸드폰번호가 유효한 값이 아니거나, 값이 "수신번호"일때 inputBox를
			// 비워주는 이벤트리스너 /////////////////////////////
			@Override
			public void focusGained(FocusEvent e) {
				if (isValidPhoneNum(inputPhoneNumBox.getText()) == null) {
					inputPhoneNumBox.setText("");
				}
			}
		});

		// 전송할 메세지 내용을 담는
		// messageBox/////////////////////////////////////////////////////////////////////////////////////////////////////////
		JTextPane messageBox = new JTextPane();
		messageBox.setBounds(27, 87, 293, 159);
		frmSinglemessagesend.getContentPane().add(messageBox);

		// "전송" 버튼을 눌렀을때
		// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		JButton btnSendMsg = new JButton("\uBCF4\uB0B4\uAE30");
		btnSendMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String blockMsgId = getMsgId(); // 전송버튼을 눌렀을때마다 다른 메세지와 구별하기위한 블럭화 ID값 생성

				// 메세지 전송을 할때, 메세지 박스가 비워져있는 경우
				if (messageBox.getText().equals("")) {
					insertResultMessage("메세지를 작성해주세요.");
				} else {
					// 푸시메세지 전송
					// 부분///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					try {
						// 푸시 메세지 전송 시점의 시간을 saveTime 변수에 저장(파일명때문에 필요)
						saveTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
						String msg = URLEncoder.encode(messageBox.getText(), "UTF-8"); // 메세지 인코드 작업
						if (receiveList.size() > 1) { // 복문
							saveDbMultiReceiverInfo(blockMsgId, receiveList, msg);
						} else { // 단문
							saveDbSingleReceiverInfo(blockMsgId, receiveList, msg); // 푸시 메세지를 전송하는 메소드에 받는사람의 정보를 담고있는
																					// 객체리스트와 메세지 내용을 매개변수로 보낸다.
						}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					// 푸시메세지 전송
					// 부분///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

					insertResultMessage("메세지를 전송하였습니다.");
				}
			}
		});

		// 핸드폰 기기타입
		// 콤보박스///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		JComboBox cbPhoneType = new JComboBox();
		cbPhoneType.setFont(new Font("굴림", Font.PLAIN, 14));
		cbPhoneType.setModel(new DefaultComboBoxModel(new String[] { "-- 선택 --", "ios", "android" }));
		cbPhoneType.setBounds(116, 293, 203, 28);
		frmSinglemessagesend.getContentPane().add(cbPhoneType);

		// 받는 사람 스크롤바
		// 추가/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		JScrollPane receiveNumScrollPane = new JScrollPane();
		receiveNumScrollPane.setBounds(465, 87, 219, 235);
		frmSinglemessagesend.getContentPane().add(receiveNumScrollPane);

		// 받는 사람 리스트
		// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		receiveListModel = new DefaultListModel();
		JList receiveNumList = new JList(receiveListModel);
		receiveNumScrollPane.setViewportView(receiveNumList); // 스크롤바에 리스트를 추가

		// 추가 버튼을
		// 눌렀을때////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		JButton btnAddNum = new JButton("추가");
		btnAddNum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ReceiveInfo ri = null;
				String phoneNum = inputPhoneNumBox.getText();
				String phoneType = cbPhoneType.getSelectedItem().toString(); // 콤보박스에서 선택된 핸드폰 타입의 값을 String으로 가져온다.
				if (phoneType.equals("-- 선택 --")) {
					insertResultMessage("기기의 타입을 선택해주세요.");
					return;
				}
				String receiveInfo = phoneNum + " " + phoneType; // 핸드폰 번호 + 핸드폰 타입

				// DB에 입력된 핸드폰 번호를 넣고 조회해서 번호, 토큰값 , 기기종류(receiveInfo)를 반환 받아 ri에 저장
				try {
					// 받는 사람 리스트에 핸드폰 번호의 형식은 010-0000-0000 이지만 DB에서의 핸드폰 번호 형식은 01000000000 임으로
					// 형식변환 작업
					String inputDBPhoneNum = phoneNum.substring(0, 3) + phoneNum.substring(4, 8)
							+ phoneNum.substring(9, 13);

					// DB에서 해당 핸드폰번호와 핸드폰 타입에 맞는 사람의 정보를 조회 후 반환
					ri = getReceiveInfo(inputDBPhoneNum, phoneType);

					if (ri == null) { // 조회 데이터가 없는경우
						insertResultMessage("해당 번호의 데이터를 찾을 수 없습니다.");
						return;
					}

				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// HashMap에 핸드폰 번호와 핸드폰 타입 저장
				// /////////////////////////////////////////////////////////////////////////////////////////////
				if (!receiveList.containsKey(phoneNum)) { // 한번 메세지를 보낼때 중복된 핸드폰 번호가 저장되는것을 방지
					receiveListModel.insertElementAt(receiveInfo, 0); // 받는사람 UI 리스트에 보내고자 하는 사람의 핸드폰번호와 핸드폰 종류를 추가
					receiveList.put(phoneNum, ri); // 받는사람의 전체 리스트를 저장하는 HashMap에 저장
					insertResultMessage(
							phoneNum + "번호가 리스트에 추가되었습니다. " + "[ " + cbPhoneType.getSelectedItem().toString() + " ]");
				} else {
					insertResultMessage(phoneNum + "번호가 리스트에 존재합니다.");
				}

			}
		});

		JLabel lblPhoneNum = new JLabel("\uD578\uB4DC\uD3F0\uBC88\uD638: ");
		lblPhoneNum.setFont(new Font("굴림", Font.PLAIN, 14));
		lblPhoneNum.setBounds(27, 255, 107, 28);
		frmSinglemessagesend.getContentPane().add(lblPhoneNum);

		JLabel lblMsg = new JLabel("\uBA54\uC138\uC9C0: ");
		lblMsg.setFont(new Font("굴림", Font.PLAIN, 14));
		lblMsg.setBounds(27, 50, 107, 28);
		frmSinglemessagesend.getContentPane().add(lblMsg);

		JLabel lblResultMessage = new JLabel("\uACB0\uACFC \uBA54\uC138\uC9C0:");
		lblResultMessage.setFont(new Font("굴림", Font.PLAIN, 14));
		lblResultMessage.setBounds(27, 337, 107, 28);
		frmSinglemessagesend.getContentPane().add(lblResultMessage);

		inputPhoneNumBox.setText("\uC218\uC2E0\uBC88\uD638");
		inputPhoneNumBox.setFont(new Font("굴림", Font.PLAIN, 14));
		inputPhoneNumBox.setBounds(116, 256, 203, 28);
		frmSinglemessagesend.getContentPane().add(inputPhoneNumBox);
		inputPhoneNumBox.setColumns(10);

		btnSendMsg.setFont(new Font("굴림", Font.PLAIN, 14));
		btnSendMsg.setBounds(350, 267, 85, 29);
		frmSinglemessagesend.getContentPane().add(btnSendMsg);

		chkShowMsg = new JCheckBox("\uBCF4\uAE30");
		chkShowMsg.setBounds(481, 341, 54, 23);
		frmSinglemessagesend.getContentPane().add(chkShowMsg);

		chkSaveMsg = new JCheckBox("\uC800\uC7A5");
		chkSaveMsg.setBounds(538, 341, 62, 23);
		frmSinglemessagesend.getContentPane().add(chkSaveMsg);

		JButton btnDelMsg = new JButton("\uC9C0\uC6B0\uAE30");
		btnDelMsg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				resultMessageModel.removeAllElements();
			}
		});
		btnDelMsg.setFont(new Font("굴림", Font.PLAIN, 14));
		btnDelMsg.setBounds(604, 337, 81, 29);
		frmSinglemessagesend.getContentPane().add(btnDelMsg);

		JLabel lblPhoneType = new JLabel("기기 타입:");
		lblPhoneType.setFont(new Font("굴림", Font.PLAIN, 14));
		lblPhoneType.setBounds(27, 293, 81, 28);
		frmSinglemessagesend.getContentPane().add(lblPhoneType);

		btnAddNum.setFont(new Font("굴림", Font.PLAIN, 14));
		btnAddNum.setBounds(350, 109, 85, 29);
		frmSinglemessagesend.getContentPane().add(btnAddNum);

		JButton btnSelectDelNum = new JButton("삭제");
		btnSelectDelNum.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (receiveNumList.getSelectedValue() != null) {
					String delPhoneNum = receiveNumList.getSelectedValue().toString(); // 받는사람 리스트에 선택된 값을 문자열로 가져온다.
					insertResultMessage("삭제된 번호: " + delPhoneNum);

					receiveList.remove(delPhoneNum.substring(0, 13));
					receiveListModel.removeElementAt(receiveNumList.getSelectedIndex());
				} else {
					insertResultMessage("삭제할 번호를 리스트에서 선택해 주세요.");
				}
			}
		});

		btnSelectDelNum.setFont(new Font("굴림", Font.PLAIN, 14));
		btnSelectDelNum.setBounds(350, 147, 85, 30);
		frmSinglemessagesend.getContentPane().add(btnSelectDelNum);

		JButton btnClearList = new JButton("초기화");
		btnClearList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				receiveListModel.removeAllElements();
				receiveList = new HashMap<String, ReceiveInfo>();

			}
		});
		btnClearList.setFont(new Font("굴림", Font.PLAIN, 14));
		btnClearList.setBounds(350, 187, 85, 30);
		frmSinglemessagesend.getContentPane().add(btnClearList);

		JButton btnDbSearch = new JButton("DB조회");
		btnDbSearch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String phoneNum = null;
				String phoneType = null;
				String receiveInfo = null;
				ReceiveInfo ri;
				ArrayList<ReceiveInfo> riArrList;

				// tb_fcm_list 테이블에 있는 모든 데이터를 가져와 riArrList에 저장한다.
				// tb_fcm_list 테이블은 고객들의 핸드폰번호, 토큰값, 핸드폰 종류등의 정보가 들어 있다.
				try {
					String inputPhoneNum = null;
					String inputPhoneType = null;

					// tb_fcm_list 테이블의 전체 내용을 ArrayList로 저장해서 리턴된 값을 riArrList에 담는다.
					riArrList = getAllReceiveInfo(inputPhoneNum, inputPhoneType);
					for (int i = 0; i < riArrList.size(); i++) {
						ri = riArrList.get(i); // ArrayList에 있는 내용들(ReceiveInfo클래스의 객체)을 하나씩 꺼내 ri에 저장한다.
						phoneNum = ri.getPhoneNum();
						receiveList.put(phoneNum, ri); // 받는사람의 전체 리스트를 저장하는 HashMap에 key(핸드폰 번호)와 value(receiveInfo)를
														// 저장

						phoneType = ri.getPhoneType();
						receiveInfo = phoneNum + phoneType;
						receiveListModel.insertElementAt(receiveInfo, 0); // 받는사람의 정보를 리스트 UI에 표시 해준다.
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		btnDbSearch.setFont(new Font("굴림", Font.PLAIN, 14));
		btnDbSearch.setBounds(350, 227, 85, 30);
		frmSinglemessagesend.getContentPane().add(btnDbSearch);

		JLabel lblReceiveNum = new JLabel("받는 사람:");
		lblReceiveNum.setFont(new Font("굴림", Font.PLAIN, 14));
		lblReceiveNum.setBounds(465, 50, 116, 28);
		frmSinglemessagesend.getContentPane().add(lblReceiveNum);
	}

	// 핸드폰 번호 유효성 검사하는
	// 메소드//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String isValidPhoneNum(String phoneNum) {

		// 핸드폰 번호를 입력하지 않았는가?
		if (phoneNum.equals("")) {
			insertResultMessage("수신번호를 입력하지 않으셨습니다.");
		}
		// 핸드폰 번호가 11자리가 아닌가?
		else if (phoneNum.length() != 11) {
			insertResultMessage("핸드폰 번호를 다시 입력해주세요.");
		}
		// 핸드폰 번호가 숫자로만 구성되어있지 않은가?
		else if (!isInteger(phoneNum)) {
			insertResultMessage("수신번호 입력시 숫자만 입력해주세요.");
		}
		// 010으로 시작한가?
		else if (!(phoneNum.substring(0, 3).equals("010"))) {
			insertResultMessage("핸드폰 번호를 다시 입력해주세요.");
		}
		// 제대로 입력 되었다면 핸드폰번호에 "-" 를 붙여준다.
		else {
			String resultPhoneNum = "";
			resultPhoneNum = phoneNum.substring(0, 3) + "-" + phoneNum.substring(3, 7) + "-"
					+ phoneNum.substring(7, 11);
			insertResultMessage("수신번호 " + resultPhoneNum + " 를 입력하셨습니다.");

			return resultPhoneNum;
		}

		return null;
	}

	// 문자열값이 정수인지 판별하는
	// 메소드//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 정수로 변환시 제대로 변환되면 true 예외처리되면 false
	// ///////////////////////////////////////////////////////////////////////////////////////////////////
	public boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		return true;
	}

	// 현재날짜 불러오는
	// 메소드/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getNowDate() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
	}

	// 현재시간을 불러오는
	// 메소드///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getNowTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
	}

	// 현재날짜와 시간을 불러오는
	// 메소드///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getNowDateTime() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	// 결과메세지를 쓰는
	// 메소드/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void insertResultMessage(String msg) {
		if (chkShowMsg.isSelected()) { // 보기 체크박스를 눌렀을때
			while (resultMessageModel.getSize() > 59) { // 결과 메세지가 60개가 넘으면 제일 오래 전에 기록된 메세지를 삭제해준다.
				resultMessageModel.removeElementAt(resultMessageModel.getSize() - 1);
			}
			resultMessageModel.insertElementAt("[" + getNowDate() + "] " + msg, 0);
		}
		if (chkSaveMsg.isSelected()) {
			writeResultLog("[" + getNowDate() + "] " + msg + "\n");
		}

	}

	// 로그파일에 결과메세지를 기록하는
	// 메소드///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void writeResultLog(String msg) {
		File file = new File("C:/msgLog/log.txt");

		try {
			FileWriter fw = new FileWriter(file, true);
			fw.write(msg);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// 메세지 일련번호 만드는
	// 메소드//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public String getMsgId() {
		String id = "";
		int cnt = 1;

		id = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		id += String.format("%05d", cnt++);

		return id;
	}

	// 단문: DB의 tb_receiver_info 테이블에 전송할 사람의 데이터를
	// 넣는다.//////////////////////////////////////////////////////////////////////////////////////////
	public void saveDbSingleReceiverInfo(String blockMsgId, HashMap<String, ReceiveInfo> receiveList, String msg)
			throws SQLException, UnsupportedEncodingException {
		Connection conn = null;
		Statement stmt = null;
		int count = 0; // DB에 sql문 입력시 변경된 row의 수

		String msgno = null;
		// int sno; identity 값임으로 넣어주지 않아도 된다.
		String phoneNum = null;
		String phoneType = null;
		String pushToken = null;
		String msgContent = null;
		int tran_status = -1;
		String date_required = null; // 날짜는 sql문 getdate() 쓰기
		String date_sent = null;
		String date_resuelt = null;
		String multicast_id = null;
		String collapse_key = null;
		String message_id = null;
		ReceiveInfo receiveInfo = null;

		msgno = blockMsgId;
		// HashMap에 있는 키(핸드폰 번혼)에 대한 value(받는사람정보)를 가져온다.
		for (String mapkey : receiveList.keySet()) {
			receiveInfo = receiveList.get(mapkey);
		}

		phoneNum = receiveInfo.getPhoneNum(); // 핸드폰 번호
		phoneType = receiveInfo.getPhoneType(); // 핸드폰 종류
		pushToken = receiveInfo.getToken();
		msgContent = URLDecoder.decode(msg, "UTF-8");
		tran_status = 0; // 전송상태 (0: 입력, 1: 보내는중, 2: 성공, 3: 실패)
		date_required = "getdate()";
		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			String sql = "insert into TB_RECEIVER_INFO(msgno, phoneNum, pushToken, phoneType, msgContent, tran_status, date_required,\r\n"
					+ "	date_sent, date_result, multicast_id, collapse_key, message_id, tran_ect1, tran_ect2, tran_ect3, tran_ect4)\r\n"
					+ "values ('" + msgno + "', '" + phoneNum + "', '" + pushToken + "', '" + phoneType + "', + '"
					+ msgContent + "', " + tran_status + ", " + date_required + ", " + date_sent + ", " + date_resuelt
					+ ", " + multicast_id + ", " + collapse_key + ", " + message_id + ", null,null,null,0);";

			count = stmt.executeUpdate(sql);

		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			insertResultMessage("DB 연결실패");
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

	// 복문: DB의 tb_receiver_info 테이블에 전송할 사람의 데이터를
	// 넣는다.//////////////////////////////////////////////////////////////////////////////////////////
	public void saveDbMultiReceiverInfo(String blockMsgId, HashMap<String, ReceiveInfo> receiveList, String msg)
			throws SQLException, UnsupportedEncodingException {
		Connection conn = null;
		Statement stmt = null;
		String sql = null;
		int count = 0; // DB에 sql문 입력시 변경된 row의 수

		String msgno = null;
		// int sno; identity 값임으로 넣어주지 않아도 된다.
		String phoneNum = null;
		String phoneType = null;
		String pushToken = null;
		String msgContent = null;
		int tran_status = -1;
		String date_required = null; // 날짜는 sql문 getdate() 쓰기
		String date_sent = null;
		String date_resuelt = null;
		String multicast_id = null;
		String collapse_key = null;
		String message_id = null;
		ReceiveInfo receiveInfo = null;

		try {
			/*
			 * String connectionUrl =
			 * "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
			 * conn = DriverManager.getConnection(connectionUrl);
			 */
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			msgno = blockMsgId;
			// HashMap에 있는 키(핸드폰 번혼)에 대한 value(받는사람정보)를 가져온다.
			for (String mapkey : receiveList.keySet()) {
				receiveInfo = receiveList.get(mapkey);
				phoneNum = receiveInfo.getPhoneNum(); // 핸드폰 번호
				phoneType = receiveInfo.getPhoneType(); // 핸드폰 종류
				pushToken = receiveInfo.getToken();
				msgContent = URLDecoder.decode(msg, "UTF-8");
				tran_status = 0; // 전송상태 (0: 입력, 1: 보내는중, 2: 성공, 3: 실패)
				collapse_key = getMsgId();
				date_required = "getdate()";

				sql = "insert into TB_RECEIVER_INFO(msgno, phoneNum, pushToken, phoneType, msgContent, tran_status, date_required,\r\n"
						+ "	date_sent, date_result, multicast_id, collapse_key, message_id, tran_ect1, tran_ect2, tran_ect3, tran_ect4)\r\n"
						+ "values ('" + msgno + "', '" + phoneNum + "', '" + pushToken + "', '" + phoneType + "', + '"
						+ msgContent + "', " + tran_status + ", " + date_required + ", " + date_sent + ", "
						+ date_resuelt + ", " + multicast_id + ", " + collapse_key + ", " + message_id
						+ ", null,null,null,0);";

				count = stmt.executeUpdate(sql);
			}

		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			insertResultMessage("DB 연결실패");
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

	// 로그파일에 전송 기록을 저장하는
	// 메소드///////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public void writeSendMsgLog(String msg) {
		String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		File file = new File("C:/msgLog/" + date + "_" + saveTime + "_" + receiveList.size() + ".log");
		try {
			FileWriter fw = new FileWriter(file, true);
			fw.write(msg);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// DB
	// 메소드////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// 한명의 핸드폰번호와 기기종류를 입력받았을때 DB조회
	// /////////////////////////////////////////////////////////////////////////////////////////////////
	public ReceiveInfo getReceiveInfo(String phoneNum, String phoneType) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		ReceiveInfo ri = null;

		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			String sql = "select PHONE, PushToken, ShortPushToken from tb_fcm_list where PHONE = '" + phoneNum
					+ "' and ShortPushToken = '" + phoneType + "'";
			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				String phone = rs.getString("phone");
				String token = rs.getString("pushToken");
				String type = rs.getString("shortPushToken");

				ri = new ReceiveInfo(phone, token, type);
			}

		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			insertResultMessage("DB 연결실패");
			JOptionPane.showMessageDialog(null, "DB 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // DB연결실패시 팝업창 뜨게함
		}

		finally { // 자원 반납
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

		return ri;

	}

	// DB 메소드
	// 구역////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// DB 테이블 내의 모든 정보를 조회하는 메소드
	// //////////////////////////////////////////////////////////////////////////////////////////////////////
	public ArrayList<ReceiveInfo> getAllReceiveInfo(String phoneNum, String phoneType) throws SQLException {
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		ReceiveInfo ri = null;
		ArrayList<ReceiveInfo> riArrList = new ArrayList<ReceiveInfo>();

		try {
//			String connectionUrl = "jdbc:sqlserver://192.168.10.192;databaseName=Anycare;user=kabsung3;password=hyung269";
//			conn = DriverManager.getConnection(connectionUrl);
			conn = cpm.getConnection();
			stmt = conn.createStatement();

			String sql = null;

			if (phoneNum == null && phoneType == null) {
				sql = "select PHONE, PushToken, ShortPushToken from tb_fcm_list";
			} else if (phoneNum != null && phoneType == null) { // phoneNum을 조건문으로 사용할때
				sql = "select PHONE, PushToken, ShortPushToken from tb_fcm_list where PHONE = '" + phoneNum + "'";
			}

			else if (phoneNum == null && phoneType != null) { // phoneType을 조건문으로 사용할때
				sql = "select PHONE, PushToken, ShortPushToken from tb_fcm_list where ShortPushToken = '" + phoneType
						+ "'";
			} else { // phoneNum과 phoneType을 조건문으로 사용할때
				sql = "select PHONE, PushToken, ShortPushToken from tb_fcm_list where PHONE = '" + phoneNum
						+ "' and ShortPushToken = '" + phoneType + "'";
			}

			rs = stmt.executeQuery(sql);

			while (rs.next()) {
				String phone = rs.getString("phone");
				String token = rs.getString("pushToken");
				String type = rs.getString("shortPushToken");

				ri = new ReceiveInfo(phone, token, type);
				riArrList.add(ri);
			}

		} catch (SQLException sqle) {
			System.out.println("DB 연결 실패");
			System.out.println("SQLException: " + sqle);
			insertResultMessage("DB 연결실패");
			JOptionPane.showMessageDialog(null, "DB 연결실패", "Message", JOptionPane.ERROR_MESSAGE); // DB연결실패시 팝업창 뜨게함
		}

		finally { // 자원 반납
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

		return riArrList;

	}

}
