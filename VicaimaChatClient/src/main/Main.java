package main;

import java.awt.Dimension;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import javax.swing.JFrame;

import client.ChatClient;

public class Main {
	public static void main(String[] args) {
		try {
			final File file = new File("single.class");
			final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
			final FileLock fileLock = randomAccessFile.getChannel().tryLock();
			if (fileLock != null) {
				ChatClient client = new ChatClient();
				client.setTitle("Vicaima Chat Client");
				client.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				client.setPreferredSize(new Dimension(600, 500));
				client.init();
				client.setLocationRelativeTo(null);
				client.pack();
				client.setVisible(true);
				Runtime.getRuntime().addShutdownHook(new Thread() {
					public void run() {
						try {
							fileLock.release();
							randomAccessFile.close();
							file.delete();
						} catch (Exception e) {
							System.out.println("Unable to remove lock file.\n" + e);
						}
					}
				});
			}
		} catch (Exception e) {
			System.out.println("Unable to create and/or lock file.\n" + e);
		}		
	}
}
