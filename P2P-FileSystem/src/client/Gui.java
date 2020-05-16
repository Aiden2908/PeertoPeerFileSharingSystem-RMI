package client;

// Java program to implement 
// a Simple Registration Form 
// using Java Swing 

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import p2p_server.Client;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Gui extends JFrame {
	private static final long serialVersionUID = 1L;
	// Components of the Form
	private Container c;
	private JLabel title;
	private JLabel lbClientID, lbMessage, lbFileToSearch;
	private JTextField inputClientID;
	private JLabel lbPortNum;
	private JTextField inputPortNum, inputDirectory, inputFileToSearch;
	private JLabel lbDirectory;
	private JList clientFileJList, peerFileJList;
	private JButton btnConnect, btnSearch;
	private JButton btnReset, btnBrowse, btnDownload;
	private DefaultListModel clientFileModel, peerFileModel;
	private String selectedClient = null;
	private Client clientInstance = null;
	private Color dark = new Color(13, 13, 13), textColor = new Color(153, 153, 153), green = new Color(102, 255, 153), orange = new Color(255, 153, 102), lightBlue = new Color(102, 255, 255);
	private int WINDOW_H = 400, WINDOW_W = 800;

	private boolean onStartup;
	private int connectClient;
	Thread t;
	
	public Gui() {
		init();
		btnDownload.setEnabled(false);
		btnSearch.setEnabled(false);
		peerFileJList.setEnabled(false);
		inputFileToSearch.setEnabled(false);
	}

	public static void main(String[] args) throws Exception {
		new Gui();
	}
	
	private void runPeer() throws NumberFormatException, RemoteException {
		clientInstance = new Client();
		t = new Thread(clientInstance); // My code -- probably remove
		t.start();
		clientInstance.clientInit(Integer.parseInt(inputPortNum.getText()), Integer.parseInt(inputClientID.getText()), inputDirectory.getText(), onStartup, connectClient);
	}

	private void onBtnConnect() {
		setMessage(textColor, "connecting....");
		btnConnect.setEnabled(false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (btnConnect.getText().equals("Disconnect")) {
						clientInstance.disconnectClient();
						clientInstance.setLeader(false);
						clientInstance.setIsRunning();
						UnicastRemoteObject.unexportObject(clientInstance.rmiRegistry, true);
						dispose();
						return;
					}
					
					runPeer();
					btnSearch.setEnabled(true);
					peerFileJList.setEnabled(true);
					inputFileToSearch.setEnabled(true);
					setMessage(green, "Connected.");
					btnConnect.setEnabled(true);
					btnConnect.setText("Disconnect");
					btnConnect.setForeground(orange);
					
				} catch (Exception e) {
					System.out.println("Error: " + e);
				}
			}
		}).start();
	}

	private void onBtnReset() {
		inputClientID.setText("");
		inputPortNum.setText("");
		inputDirectory.setText("");
		setMessage(textColor, "Reset complete...");
	}

	private void onBtnBrowse() {
		JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(new java.io.File("."));
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File f = fc.getSelectedFile();
			inputDirectory.setText(f.getPath());
		}
	}

	private void onBtnDownload() {
		setMessage(textColor, "Downloading....");
		btnDownload.setEnabled(false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (clientInstance != null) {
					try {
						clientInstance.downloadFile(Integer.parseInt(selectedClient), inputFileToSearch.getText());
						btnDownload.setEnabled(true);
						setMessage(green, "Download complete.");
					} catch (Exception e) {
						setMessage(orange, "You are not connected.");
						btnDownload.setEnabled(true);
					}
					return;
				}
				setMessage(orange, "You are not connected.");
				btnDownload.setEnabled(true);
			}
		}).start();
	}

	private void onBtnSearch() throws InterruptedException, NumberFormatException, IOException {
		if (clientInstance != null) {
			String tmp = clientInstance.searchFile(inputFileToSearch.getText(), peerFileModel);
			if (tmp.equals("")) {
				setMessage(green, inputFileToSearch.getText() + " found file in client(s): ");
			} else {
				setMessage(orange, "No such file " + inputFileToSearch.getText() + " present on the network.");
			}
			return;
		}
		setMessage(orange, "You are not connected.");
	}

	private void setMessage(Color col, String str) {
		lbMessage.setForeground(col);
		lbMessage.setText(str);
	}

	private void init() {
		this.setTitle("P2P File Sharing");
		this.setSize(WINDOW_W, WINDOW_H);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setLocationRelativeTo(null);

		c = getContentPane();
		c.setBackground(dark);
		c.setLayout(null);

		title = new JLabel("File Sharing App");
		title.setFont(new Font("Arial", Font.BOLD, 24));
		title.setSize(250, 30);
		title.setLocation(250, 30);
		title.setForeground(textColor);
		c.add(title);

		lbClientID = new JLabel("Client ID");
		lbClientID.setFont(new Font("Arial", Font.PLAIN, 14));
		lbClientID.setSize(100, 20);
		lbClientID.setLocation(25, 100);
		lbClientID.setForeground(textColor);
		c.add(lbClientID);

		inputClientID = new JTextField("1");
		inputClientID.setFont(new Font("Arial", Font.PLAIN, 14));
		inputClientID.setSize(190, 20);
		inputClientID.setLocation(100, 100);
		c.add(inputClientID);

		lbPortNum = new JLabel("Port num");
		lbPortNum.setFont(new Font("Arial", Font.PLAIN, 14));
		lbPortNum.setSize(100, 20);
		lbPortNum.setLocation(25, 125);
		lbPortNum.setForeground(textColor);
		c.add(lbPortNum);

		inputPortNum = new JTextField("8001");
		inputPortNum.setFont(new Font("Arial", Font.PLAIN, 14));
		inputPortNum.setSize(150, 20);
		inputPortNum.setLocation(100, 125);
		c.add(inputPortNum);

		lbDirectory = new JLabel("Directory");
		lbDirectory.setFont(new Font("Arial", Font.PLAIN, 14));
		lbDirectory.setSize(100, 20);
		lbDirectory.setLocation(25, 150);
		lbDirectory.setForeground(textColor);
		c.add(lbDirectory);

		inputDirectory = new JTextField("C:\\Users\\Aiden\\Documents\\audiototext\\peer1");
		inputDirectory.setFont(new Font("Arial", Font.PLAIN, 14));
		inputDirectory.setSize(200, 20);
		inputDirectory.setLocation(100, 150);
		c.add(inputDirectory);

		lbMessage = new JLabel("");
		lbMessage.setFont(new Font("Arial", Font.PLAIN, 13));
		lbMessage.setSize(WINDOW_W, 20);
		lbMessage.setLocation(50, 240);
		lbMessage.setForeground(textColor);
		c.add(lbMessage);

		btnBrowse = new JButton("Browse");
		btnBrowse.setFont(new Font("Arial", Font.PLAIN, 15));
		btnBrowse.setSize(120, 25);
		btnBrowse.setLocation(310, 150);
		btnBrowse.setBorder(new RoundedBorder(20));
		btnBrowse.setBackground(dark);
		btnBrowse.setForeground(textColor);
		btnBrowse.setFocusPainted(false);
		btnBrowse.setContentAreaFilled(false);
		btnBrowse.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onBtnBrowse();
			}
		});
		c.add(btnBrowse);

		btnConnect = new JButton("Connect");
		btnConnect.setFont(new Font("Arial", Font.PLAIN, 15));
		btnConnect.setSize(120, 25);
		btnConnect.setLocation(140, 200);
		btnConnect.setBorder(new RoundedBorder(20)); // 10 is the radius
		btnConnect.setBackground(dark);
		btnConnect.setForeground(green);
		btnConnect.setFocusPainted(false);
		btnConnect.setContentAreaFilled(false);
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onBtnConnect();
			}
		});
		c.add(btnConnect);

		btnReset = new JButton("Reset");
		btnReset.setFont(new Font("Arial", Font.PLAIN, 15));
		btnReset.setSize(120, 25);
		btnReset.setLocation(270, 200);
		btnReset.setBorder(new RoundedBorder(20)); // 10 is the radius
		btnReset.setBackground(dark);
		btnReset.setForeground(orange);
		btnReset.setFocusPainted(false);
		btnReset.setContentAreaFilled(false);
		btnReset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onBtnReset();
			}
		});
		c.add(btnReset);

		peerFileModel = new DefaultListModel();
		peerFileJList = new JList(peerFileModel);
		peerFileJList.setFont(new Font("Arial", Font.PLAIN, 15));
		peerFileJList.setSize(325, WINDOW_H / 2);
		peerFileJList.setLocation(460, 50);
		TitledBorder border = BorderFactory.createTitledBorder("File available from other peers");
		border.setTitleFont(new Font("Arial", Font.PLAIN, 13));
		peerFileJList.setBorder(border);
		peerFileJList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				if (!arg0.getValueIsAdjusting()) {
					try {
						String[] s = peerFileJList.getSelectedValue().toString().split(" ");
						if (!s.equals("You")) {
							selectedClient = s[1];
							System.out.println(selectedClient);
						}
						if (!peerFileModel.isEmpty()) {
							btnDownload.setEnabled(true);
						} else {
							btnDownload.setEnabled(false);
						}
					} catch (Exception e) {
						System.out.println("IS NULL " + e);

					}

				}
			}
		});

		btnDownload = new JButton("Download File");
		btnDownload.setFont(new Font("Arial", Font.PLAIN, 15));
		btnDownload.setSize(150, 25);
		btnDownload.setLocation(565, 275);
		btnDownload.setBorder(new RoundedBorder(20)); // 10 is the radius
		btnDownload.setBackground(dark);
		btnDownload.setForeground(lightBlue);
		btnDownload.setFocusPainted(false);
		btnDownload.setContentAreaFilled(false);
		btnDownload.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				onBtnDownload();
			}
		});
		c.add(btnDownload);

		clientFileModel = new DefaultListModel();
		clientFileJList = new JList(clientFileModel);
		clientFileJList.setFont(new Font("Arial", Font.PLAIN, 15));
		clientFileJList.setSize(250, WINDOW_H / 2 - 75);
		clientFileJList.setLocation(520, 50);
		clientFileJList.setBorder(BorderFactory.createTitledBorder("Files in your directory"));
		clientFileJList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				if (!arg0.getValueIsAdjusting()) {
					System.out.println(clientFileJList.getSelectedValue().toString());
				}
			}
		});

		c.add(peerFileJList);

		lbFileToSearch = new JLabel("File to search (located in other peers)");
		lbFileToSearch.setFont(new Font("Arial", Font.PLAIN, 20));
		lbFileToSearch.setSize(400, 20);
		lbFileToSearch.setLocation(50, 400);
		lbFileToSearch.setForeground(textColor);
		c.add(lbFileToSearch);

		inputFileToSearch = new JTextField("common.txt");
		inputFileToSearch.setFont(new Font("Arial", Font.PLAIN, 14));
		inputFileToSearch.setSize(190, 20);
		inputFileToSearch.setLocation(50, 275);
		c.add(inputFileToSearch);

		btnSearch = new JButton("Search");
		btnSearch.setFont(new Font("Arial", Font.PLAIN, 14));
		btnSearch.setSize(120, 25);
		btnSearch.setLocation(250, 275);
		btnSearch.setBorder(new RoundedBorder(20));
		btnSearch.setBackground(dark);
		btnSearch.setForeground(lightBlue);
		btnSearch.setFocusPainted(false);
		btnSearch.setContentAreaFilled(false);
		btnSearch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					onBtnSearch();
				} catch (InterruptedException | NumberFormatException | IOException e1) {
					setMessage(orange, "Error: " + e1);
				}
			}
		});
		c.add(btnSearch);
		setVisible(true);
	}

	public void setValues_TEST(int ID, int portNum, String dir, Boolean onStartup, int connectClient) {
		this.inputClientID.setText("" + ID);
		this.inputPortNum.setText("" + portNum);
		this.inputDirectory.setText(dir);
		this.onStartup = onStartup;
		this.connectClient = connectClient;
	}

	public void connect_TEST() {
		onBtnConnect();
	}

	private static class RoundedBorder implements Border {

		private int radius;

		RoundedBorder(int radius) {
			this.radius = radius;
		}

		public Insets getBorderInsets(Component c) {
			return new Insets(this.radius + 1, this.radius + 1, this.radius + 2, this.radius);
		}

		public boolean isBorderOpaque() {
			return true;
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
		}
	}
}