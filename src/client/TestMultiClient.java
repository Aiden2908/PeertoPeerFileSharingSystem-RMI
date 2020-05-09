package client;

public class TestMultiClient {
	public static void main(String[] args) throws InterruptedException {
		startClient(1,8001,"C:\\Users\\Aiden\\Documents\\audiototext\\peer1");
		Thread.sleep(2000);
		startClient(2,8002,"C:\\Users\\Aiden\\Documents\\audiototext\\peer2");
		Thread.sleep(2000);
		startClient(3,8003,"C:\\Users\\Aiden\\Documents\\audiototext\\peer3");
		Thread.sleep(2000);
		startClient(4,8004,"C:\\Users\\Aiden\\Documents\\audiototext\\peer4");
	}
	private static void startClient(int ID,int portNum,String SharedDir) {
			Gui client = new Gui();
			client.setValues_TEST(ID,portNum,SharedDir);
			client.connect_TEST();
	}
	
}