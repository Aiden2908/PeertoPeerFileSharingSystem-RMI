package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ControlPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private static JFrame frame = new JFrame("Control Panel");
    private static Toolkit tk;
    private static Dimension screenDimension, frameDimension;
    
    private DrawPanel panel;
    
    private JTextField myIDText, joinPortText;
    
    private JLabel myIDLabel, joinPortLabel, errorTextLabel;
    
    private JButton joinButton;
    
    public ControlPanel() {
        super(new BorderLayout());
        new TestMultiClient();
    	init();
    }
    
	public static void main(String[] args) throws Exception {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new ControlPanel());
        frame.pack();

        tk = Toolkit.getDefaultToolkit();
        
        screenDimension = tk.getScreenSize();
        frameDimension = frame.getSize();

        frame.setLocation((screenDimension.width - frameDimension.width) / 2, (screenDimension.height - frameDimension.height) / 2);
        frame.setSize(new Dimension(350, 200));
        frame.setVisible(true);
        frame.setResizable(true);
	}
	
	private void init() {
		panel = new DrawPanel();
		
		myIDText = new JTextField();
		myIDText.setPreferredSize(new Dimension(5, 50));
		
		joinPortText = new JTextField();	
		joinPortText.setPreferredSize(new Dimension(5, 50));
		
		joinButton = new JButton("Join Peer-To-Peer system.");
		joinButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String myID = myIDText.getText();
				String joinPort = joinPortText.getText();
				String sharedDir = "C:\\Users\\rober\\Documents\\audiototext\\peer" + myID;

				try {
					int myIDInt = Integer.parseInt(myID);
					int myPortInt = Integer.parseInt(joinPort);
					
					if(myIDInt < 1 || myIDInt > 9 || (myPortInt < 1 || myPortInt > 9)) {
						errorTextLabel.setText("Ports must be between 1-9 inclusive.");
					} else {
						System.out.println("Creating new client: " + myID);
						
						Gui client = new Gui();
						client.setValues_TEST(myIDInt, 8000 + myIDInt , sharedDir, true, myPortInt);
						client.connect_TEST();
					}
				} catch(NumberFormatException e) {
					errorTextLabel.setText("ID and PORT must be an INTEGER (1-9)");
					e.printStackTrace();
				}
			}
			
		});
		
		myIDLabel = new JLabel("My ID.");
		myIDLabel.setForeground(Color.WHITE);
		
		joinPortLabel = new JLabel("Join Port.");
		joinPortLabel.setPreferredSize(new Dimension(60, 50));
		joinPortLabel.setForeground(Color.WHITE);
		
		errorTextLabel = new JLabel();
		errorTextLabel.setForeground(Color.RED);
		
        Box eastBox = Box.createVerticalBox();
        eastBox.add(myIDText);
        eastBox.add(Box.createVerticalStrut(3));
        eastBox.add(joinPortText);
        eastBox.add(Box.createVerticalStrut(3));
        eastBox.add(joinButton);
        eastBox.add(Box.createVerticalStrut(1));
        eastBox.add(errorTextLabel);
        
        Box westBox = Box.createVerticalBox();
        westBox.add(myIDLabel);
        westBox.add(Box.createVerticalStrut(50));
        westBox.add(joinPortLabel);
        
        panel.add(westBox);
        panel.add(eastBox);

        add(panel, BorderLayout.CENTER);
	}
	
	private class DrawPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		public DrawPanel(){
            super();
            setPreferredSize(new Dimension(1250, 750));
            setBackground(Color.BLACK);
        }
    }
}
