package main;

import java.awt.Dimension;

import javax.swing.JFrame;

import client.ChatClient;

public class Main {
	public static void main(String[] args) {
		ChatClient client = new ChatClient();
		client.setTitle("Vicaima Chat Client");
		client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.setPreferredSize(new Dimension(600,500));
		client.init();
		client.setLocationRelativeTo(null);
		client.pack();
		client.setVisible(true);		
	}
}
