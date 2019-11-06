package com.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.util.Log;

/**
 * @ClassName: Client.java
 * @author zcs
 * @version V1.0
 * @Date 2019年11月5日 上午10:48:47
 * @Modification 2019年11月5日 上午10:48:47
 * @Description: TODO(用一句话描述该文件做什么)
 */
public class FileClient {
	private final static String SERVER_IP = "192.168.0.112";
	private final static int SERVER_PORT = 8999;
	protected static final String TAG = "FileClient";
	private Socket mSocket;
	private FileInputStream fis;
	private DataOutputStream dos;
	private FileSaveListener listener;
	private boolean isSending = false;//true:正在传送中，，，上次传送已结束
	private String host;
	private int prot;
	private boolean isInitSuccess;//true 初始化完成

	// 创建客户端，并指定接收的服务端IP和端口号
	public FileClient(String ip,int prot) {
		this.host = ip;
		this.prot = prot;
		
	}
	
	public void initSocket() throws UnknownHostException, IOException {
		Log.e(TAG,"initSocket");
		this.mSocket = new Socket(host, prot);//很耗时的操作
		this.isInitSuccess = true;
		Log.e(TAG,"成功连接服务端..." + SERVER_IP);
		
	}
	// 向服务端传输文件
	public void sendFile(String url) throws IOException {
		isSending = true;
		File file = new File(url);
		try {
			fis = new FileInputStream(file);
			// BufferedInputStream bi=new BufferedInputStream(new InputStreamReader(new
			// FileInputStream(file),"GBK"));
			dos = new DataOutputStream(mSocket.getOutputStream());// client.getOutputStream()返回此套接字的输出流
			// 文件名、大小等属性
			dos.writeUTF(file.getName());
			dos.flush();
			long fileLength = file.length();
			dos.writeLong(fileLength);
			dos.flush();
			// 开始传输文件
			System.out.println("======== 开始传输文件 ========");
			byte[] bytes = new byte[2048];
			int length = 0,count = 0;
			if(listener!=null) {
				listener.start(fileLength);
			}
			while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
				count +=length;
				dos.write(bytes, 0, length);
				dos.flush();
				if(listener!=null) {
					listener.save(count, fileLength);
				}
			}
			isSending = false;
			if(listener!=null) {
				listener.onSuccess();
			}
			System.out.println("======== 文件传输成功 ========");
		} catch (IOException e) {
			isInitSuccess = true;
			e.printStackTrace();
			System.out.println("客户端文件传输异常");
		} finally {
			fis.close();
			dos.close();
		}
	}
	public void setFileSaveListener(FileSaveListener listener) {
		this.listener = listener;
	}
	public interface FileSaveListener {
		public void save(long pos,long size);
		public void start(long fileLength);
		public void onSuccess();
	}
	private void waitBack() {
		new Thread() {//接收信息
			public void run() {
				try {
					DataInputStream dataInputStream = new DataInputStream(mSocket.getInputStream());
					while(true) {//一直等待接收
						Log.e(TAG, "等待接收信息");
						//以字符串类型读取
						String readUTF = dataInputStream.readUTF();
						Log.e(TAG, "接收数据-->"+readUTF);
					}
				} catch (IOException e) {
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
					DataOutputStream dataOutputStream = new DataOutputStream(mSocket.getOutputStream());
					dataOutputStream.writeUTF(message);
					Log.e(TAG, "写完数据  "+message);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			};
			
		}.start();
		return true;
	}

	public static void main(String[] args) {
		try {
			FileClient client = new FileClient(SERVER_IP,SERVER_PORT); // 启动客户端连接
			client.sendFile("D:/123.mp4"); // 传输文件
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 是否正在传输中
	 * @return true:还有未结束的
	 */
	public boolean isSending() {
		return isSending;
	}
	public void setSending(boolean isSending) {
		this.isSending = isSending;
	}
	
	public boolean isInitSuccess() {
		return isInitSuccess;
	}

	public void setInitSuccess(boolean isInitSuccess) {
		this.isInitSuccess = isInitSuccess;
	}

	public void close() {
		if(mSocket!=null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	

}
