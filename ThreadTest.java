package pushMessage_MultiThread_DBCP;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadTest {
	public static void main(String[] args) throws InterruptedException {
	
		
		
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		
		ArrayList<Integer> numList = new ArrayList<Integer>();
		for(int i = 0; i < 10; i++) {
			numList.add(i+1);
		}
		
		Runnable runnable1 = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				forPrint();
				whilePrint();
				for(int i : numList) {
					System.out.println("runnable1_numList: " + i);
				}
			}
		};
		
		executorService.execute(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				forPrint();
				whilePrint();
				for(int i : numList) {
					System.out.println("runnable2_numList: " + i);
				}
			}
		});
		Thread.sleep(1000);	
		
		//executorService.execute(runnable2);
		
		//Thread.sleep(1000);
		
		System.out.println("스레드 풀 종료합니다.");
		executorService.shutdown();
		
		
	}
	
	public static void forPrint() {
		for(int i = 0; i < 10; i++) {
			System.out.println("for문 입니다. " + (i + 1));
		}
	}
	
	public static void whilePrint() {
		int i = 0;
		
		while(true) {
			i++;
			System.out.println("while문 입니다." + i);
			
			if(i == 10) {
				break;
			}
		}
	
	}
}
