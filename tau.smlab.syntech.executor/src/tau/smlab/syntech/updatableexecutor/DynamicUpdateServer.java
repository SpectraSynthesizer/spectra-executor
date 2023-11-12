/*
Copyright (c) since 2015, Tel Aviv University and Software Modeling Lab

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Tel Aviv University and Software Modeling Lab nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Tel Aviv University and Software Modeling Lab 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT 
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
*/

package tau.smlab.syntech.updatableexecutor;

import tau.smlab.syntech.updatableexecutor.UpdatableControllerExecutor;
import tau.smlab.syntech.controller.jit.BasicJitController;
import tau.smlab.syntech.jtlv.Env;

import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import net.sf.javabdd.BDD;

import java.io.*;

public class DynamicUpdateServer extends Thread {

	protected final static String WIN = "winning.bdd";
	protected final static String RO = "trans.bdd";

	protected final static String VARS = "vars.doms";
	protected final static String SIZES = "sizes";
	protected final static String FIXPOINTS = "fixpoints.bdd";
	protected final static String TRANS = "trans.bdd";
	protected final static String JUSTICE = "justice.bdd";
	protected final static String BRIDGE_VARS = "bridgeVars.doms";
	protected final static String SYS_TRANS = "sysTrans.bdd";

	protected final static int MAX_FILE_SIZE = 6022386;
	protected final static int BLOCK_SIZE = 65536;

	protected final static String START_UPDATE = "Update";
//	protected final static String READY = "Ready";
//	protected final static String NOT_READY = "NotReady";
	protected final static String CHECK_CONNECTION = "Check";
	protected final static String IN_REGION = "Yes";
	protected final static String CONNECTED = "Connected";
	protected final static String NOT_IN_REGION = "No";
	protected final static String RECEIVED = "Received";

	protected final static int SOCKET_TIMEOUT = 10000;
	protected final static int WINNING_SOCKET_TIMEOUT = 300000;
	protected final static int ACCEPT_TIMEOUT = 5000;

	UpdatableControllerExecutor executor;
	protected int port;
	protected ServerSocket serverSocket;
	protected boolean running = true;
	PrintWriter out;
	BufferedReader in;

	public DynamicUpdateServer(UpdatableControllerExecutor executor, int port) {
		super();
		this.executor = executor;
		this.port = port;
	}

	void receiveFile(Socket socket, String path) throws IOException {
		System.out.println("Receives file " + path);
		int bytesRead;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataInputStream dis = null;
		try {
			byte [] fileBytes = new byte [BLOCK_SIZE];
			System.out.println("Allocated bytes for receiving file " + path);
			InputStream is = socket.getInputStream();
			fos = new FileOutputStream(path);
			bos = new BufferedOutputStream(fos);
			dis = new DataInputStream(is);
			long fileSize = dis.readLong();
			System.out.println("Receiving " + String.valueOf((int) fileSize) + " bytes of file " + path);
			while (fileSize > 0) {
				bytesRead = dis.read(fileBytes, 0, (int) Math.min(BLOCK_SIZE, fileSize));
				System.out.println("Received " + String.valueOf((int) bytesRead) + " bytes of file " + path);
				bos.write(fileBytes, 0, bytesRead);
				fileSize -= bytesRead;
			}
			bos.flush();
			out.println(RECEIVED);
		}
		catch (IOException e) {
			executor.printUpdateError("Failed to receive file " + path);
			throw e;
		}
		finally {
			if (fos != null) fos.close();
			if (bos != null) bos.close();
		}
	}

	void sendFile(Socket socket, String path) throws IOException {
		System.out.println("sendFile " + path);
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		OutputStream os = null;
		DataOutputStream dos = null;
		try {
			File toSend = new File(path);
			System.out.println("Created file " + path);
			byte [] fileBytes = new byte [(int)toSend.length()];
			System.out.println("Allocated bytes for " + path);
			fis = new FileInputStream(toSend);
			bis = new BufferedInputStream(fis);
			bis.read(fileBytes, 0, fileBytes.length);
			System.out.println("Read bytes from file " + path);
			os = socket.getOutputStream();
			dos = new DataOutputStream(os);
			System.out.println("Sending " + String.valueOf(fileBytes.length) + " bytes of file " + path);
			dos.writeLong(fileBytes.length);
			os.write(fileBytes, 0, fileBytes.length);
			os.flush();
			String ret = in.readLine();
			if (!ret.startsWith(RECEIVED)) {
				System.out.println("Sending file " + path + " failed!");
			}
		}
		catch (IOException e) {
			executor.printUpdateError("Failed to send file " + path);
			throw e;
		}
		finally {
			System.out.println("Finally sendFile " + path);
			if (bis != null) bis.close();
			if (fis != null) fis.close();
		}
	}

	void receiveBridge(Socket socket, BufferedReader in) throws NumberFormatException, IOException {
		String bridgeFolder = executor.getBridgeDir();
		File bridgeFile = new File(bridgeFolder);
		bridgeFile.mkdir();

		int bridgeCount = Integer.parseInt(in.readLine());
		executor.setBridgeCount(bridgeCount);
		System.out.println("Receiving bridge of size " + String.valueOf(bridgeCount));

		for (int i = 0; i < bridgeCount; i++) {
			receiveFile(socket, bridgeFolder + "bridge" + String.valueOf(i) + ".bdd");
		}

		receiveFile(socket, bridgeFolder + RO);
	}

	void receiveController(Socket socket, BufferedReader in) throws IOException {
		String outFolder = executor.getOutDir();

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("-dd-MM-yyyy-HH-mm-ss");
		LocalDateTime now = LocalDateTime.now();
		String oldFolder = outFolder + "old" + dtf.format(now);
		File oldf = new File(oldFolder);
		oldf.mkdir();

		File f = new File(outFolder + FIXPOINTS);
		f.renameTo(new File(oldFolder + File.separator + FIXPOINTS));
		receiveFile(socket, outFolder + FIXPOINTS);

		f = new File(outFolder + JUSTICE);
		f.renameTo(new File(oldFolder + File.separator + JUSTICE));
		receiveFile(socket, outFolder + JUSTICE);

		f = new File(outFolder + SIZES);
		f.renameTo(new File(oldFolder + File.separator + SIZES));
		receiveFile(socket, outFolder + SIZES);

		f = new File(outFolder + TRANS);
		f.renameTo(new File(oldFolder + File.separator + TRANS));
		receiveFile(socket, outFolder + TRANS);

		f = new File(outFolder + VARS);
		f.renameTo(new File(oldFolder + File.separator + VARS));
		receiveFile(socket, outFolder + VARS);
	}

	void removeFolder(File folder) {
	    File[] files = folder.listFiles();
	    if (files != null) { //some JVMs return null for empty dirs
	        for (File f: files) {
	            if (f.isDirectory()) {
	                removeFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}

	void handleConnection(Socket clientSocket) {
		System.out.println("Received Connection");
		try {
			clientSocket.setSoTimeout(SOCKET_TIMEOUT);
		} catch (SocketException e) {
			executor.printUpdateError("Failed to set socket timeout");
			return;
		}
	    String outFolder = executor.getOutDir();
	
	    try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {
			executor.printUpdateError("Failed to create socket reader and writer");
			return;
		};

	error : {
	    String cmd = "";
		try {
			cmd = in.readLine();
		} catch (IOException e) {
			executor.printUpdateError("Failed to receive command from client");
			break error;
		}
	    
	    System.out.println(cmd);

	    if (!cmd.startsWith(START_UPDATE)) {
		    if (cmd.startsWith(CHECK_CONNECTION)) {
				out.println(CONNECTED);
		    }
			break error;
	    }
	    
	    String winningPath = outFolder + WIN;
	    
	    try {
			clientSocket.setSoTimeout(WINNING_SOCKET_TIMEOUT);
		} catch (SocketException e) {
			executor.printUpdateError("Failed to set socket timeout");
			break error;
		}
	    
	    try {
		    sendFile(clientSocket, outFolder + SYS_TRANS);
		    sendFile(clientSocket, outFolder + VARS);
		    sendFile(clientSocket, outFolder + BRIDGE_VARS);

		    receiveFile(clientSocket, winningPath);
	    } catch (IOException e) {
	    	break error;
	    }

	    System.out.println("Received winning.bdd");
	    
	    try {
			clientSocket.setSoTimeout(SOCKET_TIMEOUT);
		} catch (SocketException e) {
			executor.printUpdateError("Failed to set socket timeout");
			break error;
		}
	    
	    boolean canUpdate = false;
		try {
			canUpdate = executor.isCurrentStateInRegion(winningPath);
		} catch (IOException e) {
			executor.printUpdateError("Failed to load winning.bdd");
			break error;
		}

	    if (canUpdate) {
	    	System.out.println("In winning region");
			out.println(IN_REGION);

			try {
				receiveBridge(clientSocket, in);
				executor.enableBridge();
			} catch (NumberFormatException | IOException e) {
				executor.printUpdateError("Failed to receive bridge.");
				executor.updateLock.unlock();
				break error;
			}
			
			try {
				receiveController(clientSocket, in);	
			} catch (IOException e) {
				executor.printUpdateError("Failed to receive new controller.");
				break error;
			}
			
			executor.lockSwitch();
			executor.updateLock.lock();

			try {
				executor.switchController(new BasicJitController());
			} catch (IllegalStateException | IOException e) {
				executor.printUpdateError("Failed to switch controller.");
				break error;
			}
	    }
	    else {
	    	System.out.println("out winning region");
			out.println(NOT_IN_REGION);
	    }

	    File f = new File(winningPath);
		if (f.exists()) {
			f.delete();
		}
	}

	    File bridgeFile = new File(executor.bridgeFolder);
	    if (bridgeFile.exists()) {
	    	removeFolder(bridgeFile);
	    }

	    try {
			in.close();
		} catch (IOException e) {
			System.out.println("Failed to close reader.");
		}
        out.close();
        System.out.println("Connection closed");
	}

	public void terminate() {
		this.running = false;
	}

	public void run() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			this.serverSocket.setSoTimeout(ACCEPT_TIMEOUT);

			Socket clientSocket;

			while (this.running) {
				try {
					clientSocket = this.serverSocket.accept();
					handleConnection(clientSocket);
					clientSocket.close();
				} catch (SocketTimeoutException e) {
					// TODO Auto-generated catch block
				}
			}

			this.serverSocket.close();
			System.out.println("Server socket is closed.");

		} catch (IOException e) {
			executor.printUpdateError("Failed to create server socket");
		}
    }
}
