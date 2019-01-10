package server;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

public class ChatServer implements Runnable {
	private ChatServerThread clients[] = new ChatServerThread[50];
	Map<Integer, String> clientsMap = new HashMap<Integer, String>();
	private ServerSocket server = null;
	private Thread thread = null;
	private int clientCount = 0;

	public ChatServer(int port) {
		try {
			System.out.println("Binding to port " + port + ", please wait  ...");
			server = new ServerSocket(port);
			System.out.println("Server started: " + server);
			start();
		} catch (IOException ioe) {
			System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
		}
	}

	public void run() {
		while (thread != null) {
			try {
				System.out.println("Waiting for a client ...");
				addThread(server.accept());
			} catch (IOException ioe) {
				System.out.println("Server accept error: " + ioe);
				stop();
			}
		}
	}

	public void start() {
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop() {
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	private int findClient(int ID) {
		for (int i = 0; i < clientCount; i++)
			if (clients[i].getID() == ID)
				return i;
		return -1;
	}

	public synchronized void handle(int ID, String input) {
		if (input.endsWith(".bye") && !input.contains("$")) {
			String clientName = input.substring(0, input.indexOf("@"));
			clients[findClient(ID)].send(".bye");
			clientsMap.remove(ID);
			remove(ID);
			for (int i = 0; i < clientCount; i++) {
				clients[i].send("Client " + clientName + " has left the chat.");
			}
		} else if (input.endsWith(".greet") && !input.contains("$")) {
			String clientName = input.substring(0, input.indexOf("@"));
			int id = findClient(ID);
			if (id != -1) {
				clientsMap.put(ID, clientName);
			}
			for (int i = 0; i < clientCount; i++) {
				if (i != id) {
					clients[i].send("Client " + clientName + " has joined the chat.");
				}
			}
			String onUsers = getOnlineUsers(ID);
			if (onUsers.length() > 2) {
				clients[id].send(onUsers);
			}
		} else if (input.endsWith(".getAllOnline") && !input.contains("$")) {
			String clientName = input.substring(0, input.indexOf("@"));
			int id = findClient(ID);
			if (id != -1) {
				clientsMap.put(ID, clientName);
			}
			String onUsers = getOnlineUsers(ID);
			if (onUsers.length() > 2) {
				clients[id].send(onUsers);
			}
		} else {
			if (input.contains("\n#<") && input.contains(">#")) {
				String clientName = input.substring(0, input.indexOf("@"));
				String begin = input.substring(0, input.indexOf("#<"));
				String middle = input.substring(input.indexOf("#<"), input.indexOf(">#") + 2);
				String end = input.substring(input.indexOf(">#") + 2, input.length());
				String[] arrOfStr = middle.split("><");
				if (arrOfStr.length < 2) {
					arrOfStr[0] = arrOfStr[0].substring(2, arrOfStr[0].length() - 2);
				} else {
					arrOfStr[0] = arrOfStr[0].substring(2, arrOfStr[0].length());
					arrOfStr[arrOfStr.length - 1] = arrOfStr[arrOfStr.length - 1].substring(0,
							arrOfStr[arrOfStr.length - 1].length() - 2);
				}
				String complete = "PRIVATE MESSAGE FROM " + clientName + " TO ";
				complete += arrOfStr[0];
				for (int i = 1; i < arrOfStr.length; i++) {
					complete += ", " + arrOfStr[i];
				}
				complete += ".\n" + begin + end;
				int[] privateClients = new int[arrOfStr.length + 1];
				privateClients[0] = findClient(getKeyFromValue(clientsMap, clientName));
				boolean foundUsers = true;
				for (int i = 0; i < arrOfStr.length; i++) {
					privateClients[i + 1] = findClient(getKeyFromValue(clientsMap, arrOfStr[i]));
					for(int j=0; j<i+1; j++) {
						if(privateClients[i+1]==privateClients[j]) {
							foundUsers = false;
						}
					}
					if (privateClients[i + 1] == -1) {
						foundUsers = false;
					}
				}
				if (foundUsers) {
					for (int i = 0; i < privateClients.length; i++) {
						clients[privateClients[i]].send(complete);
					}
				} else {
					clients[privateClients[0]].send("Client(s) incorrect or repeated. Private message not sent.\n");
				}
			} else {
				for (int i = 0; i < clientCount; i++) {
					// clients[i].send(ID + ": " + input);
					clients[i].send(input);
				}
			}
		}
	}

	public static int getKeyFromValue(Map<Integer, String> hm, Object value) {
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				return (int) o;
			}
		}
		return -1;
	}

	private String getOnlineUsers(int ID) {
		String all = "";
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() != ID) {
				all += clientsMap.get(clients[i].getID()) + "&";
			}
		}
		return all;
	}

	public synchronized void forcedQuit(int ID) {
		for (int i = 0; i < clientCount; i++) {
			if (clientsMap.get(ID) != null) {
				clients[i].send("Client " + clientsMap.get(ID) + " has left the chat.");
			}
		}
		clientsMap.remove(ID);
	}

	public synchronized void remove(int ID) {
		int pos = findClient(ID);
		if (pos >= 0) {
			ChatServerThread toTerminate = clients[pos];
			System.out.println("Removing client thread " + ID + " at " + pos);
			if (pos < clientCount - 1)
				for (int i = pos + 1; i < clientCount; i++)
					clients[i - 1] = clients[i];
			clientCount--;
			try {
				toTerminate.close();
			} catch (IOException ioe) {
				System.out.println("Error closing thread: " + ioe);
			}
		}
	}

	private void addThread(Socket socket) {
		if (clientCount < clients.length) {
			System.out.println("Client accepted: " + socket);
			clients[clientCount] = new ChatServerThread(this, socket);
			try {
				clients[clientCount].open();
				clients[clientCount].start();
				clientCount++;
			} catch (IOException ioe) {
				System.out.println("Error opening thread: " + ioe);
			}
		} else
			System.out.println("Client refused: maximum " + clients.length + " reached.");
	}
}
