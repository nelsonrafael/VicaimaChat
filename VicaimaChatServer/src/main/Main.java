package main;

import server.ChatServer;

public class Main {
	public static void main(String args[]) {
		/*
		 * ChatServer server = null; if (args.length != 1)
		 * System.out.println("Usage: java ChatServer port"); else server = new
		 * ChatServer(Integer.parseInt(args[0]));
		 */
		ChatServer server = new ChatServer(4444);
	}
}
