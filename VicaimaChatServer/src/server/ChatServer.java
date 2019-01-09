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
		if (input.endsWith(".bye")) {
			String clientName = input.substring(0, input.indexOf("@"));
			clients[findClient(ID)].send(".bye");
			clientsMap.remove(ID);
			remove(ID);
			for (int i = 0; i < clientCount; i++) {
				clients[i].send("Client " + clientName + " has left the chat.");
			}

		} else if (input.endsWith(".greet")) {
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
			clients[id].send(getOnlineUsers(ID));
		} else
			for (int i = 0; i < clientCount; i++) {
				// clients[i].send(ID + ": " + input);
				clients[i].send(input);
			}
	}
	
	private String getOnlineUsers(int ID) {
		String all = "";
		for (int i = 0; i < clientCount; i++) {
			if (clients[i].getID() != ID) {
				all += clientsMap.get(clients[i].getID())+"&";
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
