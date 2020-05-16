package client;

public class TestMultiClient {
	
	public TestMultiClient() {
		try {
			startClient(1,8001,"C:\\Users\\Aiden\\Documents\\audiototext\\peer1");
			Thread.sleep(1000);
			startClient(2,8002,"C:\\Users\\Aiden\\Documents\\audiototext\\peer2");
			Thread.sleep(1000);
			startClient(3,8003,"C:\\Users\\Aiden\\Documents\\audiototext\\peer3");
			Thread.sleep(1000);
			startClient(4,8004,"C:\\Users\\Aiden\\Documents\\audiototext\\peer4");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	private static void startClient(int ID, int portNum, String sharedDir) {
		Gui client = new Gui();
		client.setValues_TEST(ID, portNum, sharedDir, false, 0);
		client.connect_TEST();
	}
}