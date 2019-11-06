package com.wifi.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

/**
 * @ClassName:     SocketUtils.java
 * @author         zcs
 * @version        V1.0  
 * @Date           2019年11月4日 上午9:44:40
 * @Modification   2019年11月4日 上午9:44:40 
 * @Description:   TODO(用一句话描述该文件做什么) 
 */
public class SocketUtils {
	private Socket mSocket;
	private String hostAddress;
	private String hostName;
	private DataOutputStream dataOutputStream;
	private DataInputStream dataInputStream;
	String TAG = "SocketUtils";
	private ServerSocket mServerSocket;
	private boolean isSuccess;
	private SocketListener listener;
	private int port;
	/**
	 * 打开服务端
	 * @param port
	 * @throws IOException
	 */
	public SocketUtils(int port) throws IOException {
		this.port = port;
		mServerSocket = new ServerSocket(port);
	}
	/**
	 * 连接服务端
	 * @param ip
	 * @param port
	 * @throws IOException
	 */
	public SocketUtils(String host,int port) throws IOException {
		this.port = port;
		hostAddress = host;
		mSocket = new Socket(host, port);
		waitBack();
	}
	/**
	 * 
	 * @param socket 从服务器拿的
	 */
	public SocketUtils(Socket socket) {
		this.mSocket = socket;
//		int port = mSocket.getPort();
		waitBack();
		
	}
	public void initServer() {
		new Thread() {
			public void run() {
//				Log.e(TAG, "hostAddress "+hostAddress);
//				Log.e(TAG, "hostName "+hostName);
//				Log.e(TAG, "开启服务");
				try {
					// 等待客户端的连接，Accept会阻塞，直到建立连接，
					// 所以需要放在子线程中运行。
					Log.e(TAG, "服务端进入等待");
					isSuccess = true;
					mSocket = mServerSocket.accept();
					InetAddress inetAddress = mSocket.getInetAddress();
					hostAddress = inetAddress.getHostAddress();
					hostName = inetAddress.getHostName();
					port = mSocket.getPort();
					Log.e(TAG, hostAddress+"连接成功");
				} catch (IOException e) {
					e.printStackTrace();
					isSuccess = false;
					Log.e(TAG, "run: ==============" + "accept error");
					return;
				}
				isSuccess = true;
				Log.e(TAG, "accept success==================");
				// 启动消息接收线程
				waitBack();

			};

		}.start();

	}
	private void waitBack() {
		new Thread() {//接收信息
			public void run() {
				try {
					dataInputStream = new DataInputStream(mSocket.getInputStream());
					while(true) {//一直等待接收
						Log.e(TAG, "等待接收信息");
						//以字符串类型读取
						isSuccess = true;
						String readUTF = dataInputStream.readUTF();
						Log.e(TAG, "接收数据-->"+readUTF);
						if(listener!=null) {
							listener.stream(readUTF);
						}
					}
				} catch (IOException e) {
					isSuccess = false;
					e.printStackTrace();
				}
				
			};
			
		}.start();
	}	
	/**
	 * 发送数
	 * @param message
	 */
	public boolean send(final String message) {
		if(mSocket==null) {
			Log.e(TAG, "send  mSocket is NULL  "+message);
			return false;
		}
		new Thread() {//写入信息
			public void run() {
				try {
					dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
					dataOutputStream.writeUTF(message);
					Log.e(TAG, "写完数据  "+message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			};
			
		}.start();
		return true;
	}
	public void close() {
		isSuccess = false;
		if(dataInputStream!=null) {
			try {
				dataInputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(dataOutputStream!=null) {
			try {
				dataOutputStream.flush();
				dataOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(mSocket!=null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(mServerSocket!=null) {
			try {
				mServerSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	// 向服务端传输文件
	public void sendFile(String url) throws IOException {
		File file = new File(url);
		FileInputStream fis = null;
		DataOutputStream dos = null;
		try {
			fis = new FileInputStream(file);
			// BufferedInputStream bi=new BufferedInputStream(new InputStreamReader(new
			// FileInputStream(file),"GBK"));
			dos = new DataOutputStream(dataOutputStream);// client.getOutputStream()返回此套接字的输出流
			// 文件名、大小等属性
			dos.writeUTF(file.getName());
			dos.flush();
			dos.writeLong(file.length());
			dos.flush();
			// 开始传输文件
			System.out.println("======== 开始传输文件 ========");
			byte[] bytes = new byte[1024];
			int length = 0;

			while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
				dos.write(bytes, 0, length);
				dos.flush();
			}
			System.out.println("======== 文件传输成功 ========");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("客户端文件传输异常");
		} finally {
			if(fis!=null) {
				fis.close();
			}
			if(dos!=null) {
				dos.close();
			}
		}
	}
	
//	public void saveFile() {
//		
//	}
	
	
	

	public Socket getSocket() {
		return mSocket;
	}

	public void setSocket(Socket socket) {
		this.mSocket = socket;
	}

	public String getHostAddress() {
		return getSocket().getInetAddress().getHostAddress();
	}

	public void setHostAddress(String hostAddress) {
		this.hostAddress = hostAddress;
	}

	public String getHostName() {
		return getSocket().getInetAddress().getHostName();
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public DataOutputStream getDataOutputStream() {
		return dataOutputStream;
	}

	public void setDataOutputStream(DataOutputStream dataOutputStream) {
		this.dataOutputStream = dataOutputStream;
	}

	public DataInputStream getDataInputStream() {
		return dataInputStream;
	}

	public void setDataInputStream(DataInputStream dataInputStream) {
		this.dataInputStream = dataInputStream;
	}
	
	public void setMessageListener(SocketListener listener) {
		this.listener = listener;
	}
	public interface SocketListener{
		public void stream(String hostAddress);

	}
	public boolean isSuccess() {
		return isSuccess;
	}
	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	
	
	
	
	
	
	
	

}
