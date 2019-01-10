package main;

import java.awt.Dimension;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import javax.swing.JFrame;

import client.ChatClient;

public class Main {
	public static void main(String[] args) {
		try {
			@SuppressWarnings("resource")
			RandomAccessFile randomFile = new RandomAccessFile("single.class", "rw");
			FileChannel channel = randomFile.getChannel();
			if (channel.tryLock() == null)
				System.out.println("Already Running...");
			else {
				ChatClient client = new ChatClient();
				client.setTitle("Vicaima Chat Client");
				client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				client.setPreferredSize(new Dimension(600, 500));
				client.init();
				client.setLocationRelativeTo(null);
				client.pack();
				client.setVisible(true);
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
}
