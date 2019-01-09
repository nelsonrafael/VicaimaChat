package client;

import java.net.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import java.io.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChatClient extends JFrame implements ActionListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Socket socket = null;
	private DataOutputStream streamOut = null;
	private ChatClientThread client = null;
	private JTextPane display = new JTextPane();
	private JScrollPane verticalDisplayPane = new JScrollPane(display);
	private JTextArea input = new JTextArea();
	private JScrollPane verticalInputPane = new JScrollPane(input);
	private JTextPane onlineUsers = new JTextPane();
	private JScrollPane verticalOnlineUsersPane = new JScrollPane(onlineUsers);
	private JSplitPane displayAndUsers = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, verticalDisplayPane,
			verticalOnlineUsersPane);
	private JButton send = new JButton("Send"), connect = new JButton("Connect"), quit = new JButton("Quit");
	private Panel keys = new Panel();
	private String serverName = "192.168.14.148";
	private int serverPort = 4444;
	private String computerName = null;
	private String userName = null;

	public void init() {
		getComputerAndUserNames();
		keys.setLayout(new GridLayout(1, 2));
		keys.add(quit);
		keys.add(connect);
		Panel south = new Panel();
		south.setLayout(new BorderLayout());
		south.add("West", keys);
		south.add("Center", verticalInputPane);
		south.add("East", send);
		displayAndUsers.setDividerLocation(420);
		Label title = new Label("Vicaima Chat Client", Label.CENTER);
		title.setFont(new Font("Helvetica", Font.BOLD, 14));
		setLayout(new BorderLayout());
		add("North", title);
		add("Center", displayAndUsers);
		// add("Center", verticalOnlineUsersPane);
		add("South", south);
		DefaultCaret verticalDisplayCaret = (DefaultCaret) display.getCaret();
		verticalDisplayCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		input.setLineWrap(true);
		input.setWrapStyleWord(true);
		DefaultCaret verticalInputCaret = (DefaultCaret) input.getCaret();
		verticalInputCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		input.setDocument(new JTextAreaLimit(300));
		display.setFocusable(false);
		onlineUsers.setFocusable(false);
		input.addKeyListener(this);
		send.addActionListener(this);
		connect.addActionListener(this);
		quit.addActionListener(this);
	}

	private void getComputerAndUserNames() {
		try {
			this.computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
			stop(3);
		}
		this.userName = System.getProperty("user.name");
	}

	public void connect(String serverName, int serverPort) {
		println("Establishing connection. Please wait ...");
		try {
			socket = new Socket(serverName, serverPort);
			println("Connected: " + socket);
			open();
		} catch (UnknownHostException uhe) {
			println("Host unknown: " + uhe.getMessage());
		} catch (IOException ioe) {
			println("Unexpected exception: " + ioe.getMessage());
		}
	}

	private void send() {
		if (!(input.getText().equals("") || input.getText().matches("[\n]+"))) {
			try {
				int i = 0, z = input.getText().length() - 1;
				for (; i < input.getText().length(); i++) {
					if (input.getText().charAt(i) != '\n') {
						break;
					}
				}
				for (; z > i; z--) {
					if (input.getText().charAt(z) != '\n') {
						break;
					}
				}
				String parsed = input.getText().substring(i, z + 1);
				java.util.Date date = new java.util.Date();
				streamOut.writeUTF(this.userName + "@" + this.computerName + "$ " + date + "\n" + parsed);
				streamOut.flush();
			} catch (IOException ioe) {
				println("Sending error: " + ioe.getMessage());
				close();
			}
		}
		input.setText("");
	}

	public void handle(String msg) {
		if (msg.equals(".bye")) {
			println("Good bye. Press RETURN to exit ...");
			close();
			stop(0);
		} else
			println(msg);
	}

	public void open() {
		try {
			streamOut = new DataOutputStream(socket.getOutputStream());
			client = new ChatClientThread(this, socket);
		} catch (IOException ioe) {
			println("Error opening output stream: " + ioe);
		}
	}

	public void close() {
		try {
			if (streamOut != null)
				streamOut.close();
			if (socket != null)
				socket.close();
		} catch (IOException ioe) {
			println("Error closing ...");
		}
		client.close();
		stop(0);
	}

	private void println(String msg) {
		if (msg.contains("$")) {
			String firstLine = msg.substring(0, msg.indexOf('\n'));
			String fUser = msg.substring(0, firstLine.indexOf('@'));
			appendToPane(display, fUser, Color.blue, Color.white, true);
			String fComputer = msg.substring(firstLine.indexOf('@'), firstLine.indexOf('$'));
			appendToPane(display, fComputer, Color.red, Color.white, true);
			String fTime = msg.substring(firstLine.indexOf('$'), firstLine.length());
			appendToPane(display, fTime, Color.gray, Color.white, true);
			String otherLine = msg.substring(msg.indexOf('\n'), msg.length());
			appendToPane(display, otherLine + "\n", Color.black, Color.white, false);
		} else if (msg.contains("&")) {
			String[] arrOfStr = msg.split("&");
			for (String a : arrOfStr)
				appendToPane(onlineUsers, a + "\n", Color.black, Color.white, true);
		} else {
			if(msg.contains("joined")) {
				String joinedClient=msg.substring(7, msg.length()-21);
				appendToPane(onlineUsers, joinedClient + "\n", Color.black, Color.white, true);
			}else if(msg.contains("left")) {
				String leftClient=msg.substring(7, msg.length()-19);
				int index = onlineUsers.getText().indexOf(leftClient);
				onlineUsers.select(index, index+leftClient.length()+1);
				onlineUsers.replaceSelection("");
				
			}
			appendToPane(display, msg + "\n", Color.black, Color.yellow, true);
		}
	}

	private void appendToPane(JTextPane tp, String msg, Color c, Color cb, boolean b) {
		StyleContext sc = StyleContext.getDefaultStyleContext();
		AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, c);
		aset = sc.addAttribute(aset, StyleConstants.Background, cb);
		aset = sc.addAttribute(aset, StyleConstants.FontFamily, "Monospace");
		aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		aset = sc.addAttribute(aset, StyleConstants.Bold, b);
		int len = tp.getDocument().getLength();
		tp.setCaretPosition(len);
		tp.setCharacterAttributes(aset, false);
		tp.replaceSelection(msg);
	}

	public void stop(int n) {
		System.exit(n);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == connect && socket == null) {
			connect(serverName, serverPort);
			if (socket != null) {
				input.setText(".greet");
				send();
			}
		} else if (e.getSource() == quit) {
			if (this.socket != null) {
				input.setText(".bye");
				send();
			} else
				stop(0);
		} else if (e.getSource() == send && this.socket != null) {
			send();
			input.requestFocus();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		if (e.getComponent() == input) {
			int key = e.getKeyCode();
			if (key == KeyEvent.VK_ENTER && socket != null) {
				send();
				input.requestFocus();
				input.setText("");
				e.consume();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	class JTextAreaLimit extends PlainDocument {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int limit;

		JTextAreaLimit(int limit) {
			super();
			this.limit = limit;
		}

		JTextAreaLimit(int limit, boolean upper) {
			super();
			this.limit = limit;
		}

		public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
			if (str == null)
				return;

			if ((getLength() + str.length()) <= limit) {
				super.insertString(offset, str, attr);
			}
		}
	}
}
