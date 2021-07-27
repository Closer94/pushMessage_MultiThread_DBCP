package pushMessage_MultiThread_DBCP;

import lombok.*;

@AllArgsConstructor //모든 필드 값을 파라미터로 받는 생성자를 만들어줌
//@Getter
//@Setter
@Data
public class ReceiveInfo {
	 private String phoneNum;
	 private String token; 
	 private String phoneType;
	
	
}
