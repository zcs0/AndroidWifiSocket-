package com.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

/**
 * @ClassName: FileServer.java
 * @author zcs
 * @version V1.0
 * @Date 2019年11月5日 上午10:49:17
 * @Modification 2019年11月5日 上午10:49:17
 * @Description: 
 */
public class FileServer {
	private static final int SERVER_PORT = 8999; // 服务端端口
	private static final String TAG = "FileServer";
	private ServerSocket server;
	private Socket mSocket;
	private DataInputStream dis;
	private FileOutputStream fos;
	private FileSaveListener listener;
	
	private String filePath;//文件保存位置

	public FileServer(int prot,String filePath) throws Exception {
		this.filePath = filePath;
		server = new ServerSocket(prot);
		if(!new File(filePath).isDirectory()) {
			new File(filePath).mkdir();
		}
		
	}

	public void task() throws IOException {
		if(server==null) {
			Log.e(TAG,"未连接");
			throw new RuntimeException("ServerSocket 创建失败");
		}
		Log.e(TAG, "======== 等待连接 ========");
		mSocket = server.accept();
		Log.e(TAG, " Ip:" + mSocket.getInetAddress() + "已连接");
		try {
			dis = new DataInputStream(mSocket.getInputStream());
			// 文件名和长度
			String fileName = dis.readUTF();
			long fileLength = dis.readLong();
			File file = new File(filePath + File.separatorChar + fileName);
			if(file.isFile()) {//删除原有的
				file.delete();
			}
			fos = new FileOutputStream(file);
			Log.e(TAG, "file。。。。。。。。。。。。。。" + file);
			Log.e(TAG, "fileName。。。。。。。。。。。。。。" + fileName);

			Log.e(TAG, "======== 开始接收文件 ========");
			byte[] bytes = new byte[2048];
			int length = 0,count=0;
			if(listener!=null) {
				listener.start(fileLength);
			}
			while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
				count +=length;
				fos.write(bytes, 0, length);
				fos.flush();
				if(listener!=null) {
					listener.save(count, fileLength);
				}
			}
			Log.e(TAG, "======== 文件接收成功 ========"+filePath + File.separatorChar + fileName);
			if(listener!=null) {
				listener.onSuccess();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null)
					fos.close();
				if (dis != null)
					dis.close();
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
	public void setFileSaveListener(FileSaveListener listener) {
		this.listener = listener;
	}
	public interface FileSaveListener {
		public void save(long pos,long size);
		public void start(long size);
		public void onSuccess();
	}
	
	

	public static void main(String[] args) {
		try {
			FileServer server = new FileServer(SERVER_PORT,"d:/a"); // 启动服务端
			server.task();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() {
		if(mSocket!=null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(server!=null) {
			try {
				server.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
