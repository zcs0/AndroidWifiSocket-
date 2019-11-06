package com.wifi;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.server.FileClient;
import com.server.FileServer;
import com.server.FileServer.FileSaveListener;
import com.wifi.utils.NetWorkUtil;
import com.wifi.utils.ScanDeviceTool;
import com.wifi.utils.ScanDeviceTool.ScanDevicesListener;
import com.wifi.utils.SocketUtils;
import com.wifi.utils.SocketUtils.SocketListener;
import com.wifi.utils.SystemUtil;
import com.wifi.utils.inf.Const;
import com.z.utils.UriUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * @ClassName: MainActivity.java
 * @author zcs
 * @version V1.0
 * @Date 2019年11月1日 下午4:08:58
 * @Modification 2019年11月1日 下午4:08:58
 * @Description: TODO(用一句话描述该文件做什么)
 */
public class MainActivity extends Activity {
	protected String TAG = "MainActivity";
	private List<Socket> listData;
	private MyAdapter adapter;
	private int serverPort = 8988;
	private int port1 = 8988;
	private ListView lv_view;
	private SocketUtils sockerServer;
	private SocketUtils sockerClient;
	private FileClient fileClient;
	private FileServer fileServer;
	private TextView tvMessge;
	private ProgressBar progressBar;
	private List<Socket> deviceList;
	private ProgressDialog progressDialog;
	private AlertDialog dialog;
	int REQUEST_CODE = 202;
	private int send_file_port = 8888;
	boolean isFile = false;
	String fileName = null;
	private String savePath = Environment.getExternalStorageDirectory() + File.separator + "_0";// 接收文件保存位置
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TextView tv = (TextView) findViewById(R.id.tv_ip);
		tvMessge = (TextView) findViewById(R.id.tv_messge);
		tv.setText(NetWorkUtil.getIPAddress(this));
		openServer(serverPort);// 打开服务
		tvMessge.setMovementMethod(ScrollingMovementMethod.getInstance());

	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_scan:
			
			scanDevice();
			if(dialog!=null && deviceList!=null && deviceList.size()>0) {
				dialog.show();
			}
			break;
		case R.id.btn_send_file:
			if (sockerClient != null) {
				openSelect();
			}
			break;
		default:
			break;
		}
	}

	private void openSelect() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		this.startActivityForResult(intent, REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && data != null) {
			String path = UriUtils.getPath(this, data.getData());
			sendFile(path);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	

	/**
	 * 发送文件
	 * 
	 * @param path
	 */
	private void sendFile(final String path) {
		if(fileClient!=null&&fileClient.isSending()) {
			Log.e(TAG, "上次发送未结束");
			return;
		}
		if (sockerServer != null && sockerServer.isSuccess() && sockerClient != null && sockerClient.isSuccess()) {
			//通知服务端开启文件接收服务
			boolean send = sockerClient.send("sendFile:" + path);
//			if(fileClient!=null && fileClient.isSending()) {
//				Log.e(TAG, "有文件未结束");
//				return;
//			}
			if(fileClient==null) {
				fileClient = new FileClient(sockerClient.getHostAddress(), send_file_port);
			}
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1000);//防止接收的服务未开启
						startSendFile(path);
					} catch (Exception e) {
						if(fileClient!=null) {
							fileClient.close();
							fileClient = null;
						}
						e.printStackTrace();
					}
				};

			}.start();
		}
	}

	private void startSendFile(String path) throws IOException {
		Log.e(TAG, "startSendFile");
		if(!fileClient.isInitSuccess()) {
			fileClient.initSocket();
		}
		Log.e(TAG, "发送文件的链接成功");
		if(fileClient==null) {
			Log.e(TAG, "创建FileClient失败");
			return;
		}
		fileClient.setFileSaveListener(new FileClient.FileSaveListener() {
			@Override
			public void save(long pos, long size) {
//				Log.e(TAG, "发送进度：" + pos + "  size:" + size);
			}

			@Override
			public void onSuccess() {
				fileClient.close();
				fileClient = null;
				Log.e(TAG, "------------发送文件完成-------------");
			}

			@Override
			public void start(long fileLength) {
				Log.e(TAG, "开始发送 size "+fileLength);
				
				
			}
		});

//		Thread.sleep(1000);
		fileClient.sendFile(path);
	}

	/**
	 * 打开会话服务
	 * 
	 * @param port
	 */
	private void openServer(int port) {
		Log.e(TAG, "开启服务 " + port);
		try {
			if (sockerServer == null) {
				sockerServer = new SocketUtils(port);
			}
			sockerServer.initServer();
			// 接收的信息
			sockerServer.setMessageListener(new SocketListener() {
				@Override
				public void stream(final String hostAddress) {
					Log.e(TAG, "服务端收到 ：" + hostAddress);
					String serverInfo = getServerInfo(hostAddress);
					Log.e(TAG, "服务端发送数据 ：" + serverInfo);
					sockerServer.send(serverInfo);
					runOnUiThread(new Runnable() {
						public void run() {
							tvMessge.append("服务端接收：" + hostAddress + "\n");
						}
					});
					Log.e(TAG, "客户端收到信息  " + hostAddress);

				}

			});

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFileList() {
		File main = Environment.getExternalStorageDirectory();
		List<String> list = new ArrayList<String>();
		File[] listFiles = main.listFiles();
		StringBuffer sb = new StringBuffer();
//		FXJson json = new FXJson(list);
		for (File file : listFiles) {
//			list.add(file.getPath());
//			json.put(arg0, file);
			sb.append(file.getPath());
			sb.append("\n");
		}
		return sb.toString();

	}

	/**
	 * 开始扫描
	 */
	private void scanDevice() {
		progressDialog = new ProgressDialog(this);
		progressDialog.show();
		Log.e(TAG, "scanDevice  ");
		ScanDeviceTool tool = new ScanDeviceTool();
		tool.setScanDevicesListener(new ScanDevicesListener() {
			@Override
			public void ok(String ip, int count) {
				try {
					InetAddress host = InetAddress.getByName(ip);
					Log.e(TAG, "ip: " + ip);
					if(deviceList!=null) {
						for (Socket i : deviceList) {
							if(i.getInetAddress().getHostAddress().equals(ip)) {
								return;//已存在
							}
						}
					}
					final Socket socket = new Socket(host, port1);
					Log.e(TAG, "连接成功  " );
					runOnUiThread(new Runnable() {
						public void run() {
							setSelectData(socket);
						}
					});
				} catch (Exception e) {

					e.printStackTrace();
				}
			}
		});
		tool.start();

	}

	private void setSelectData(Socket ip) {
		if(dialog!=null && dialog.isShowing()) {
			dialog.dismiss();
			dialog = null;
		}
		progressDialog.dismiss();
		if (deviceList == null) {
			deviceList = new ArrayList<>();
		}
		if (!deviceList.contains(ip)) {
			deviceList.add(ip);
		}

		View view = View.inflate(this, R.layout.dialog_layout, null);
		AlertDialog.Builder builder;
//		if(dialog==null) {
		builder = new Builder(this);
//		}

		ListView lvView = (ListView) view.findViewById(R.id.lv_view);
		MyAdapter adapter = new MyAdapter();
		lvView.setAdapter(adapter);
		adapter.setData(deviceList);
		lvView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Socket socket = deviceList.get(position);
				Log.e(TAG, "socket close:"+socket.isClosed());
				Log.e(TAG, "socket bound:"+socket.isBound());
				Log.e(TAG, "socket connect:"+socket.isConnected());
				Log.e(TAG, "socket shutdown:"+socket.isInputShutdown());
				Log.e(TAG, "socket shutdown:"+socket.isOutputShutdown());
				connentSocket(socket);
				dialog.dismiss();
			}
		});
		builder.setView(view);
//		builder.setNegativeButton("确认", null);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
			}
		});
		dialog = builder.show();
//		dialog.setCancelable(false);

	}

	/**
	 * 连接到服务器
	 * 
	 * @param socket 从服务端拿到的
	 */
	private void connentSocket(Socket socket) {
		Log.e(TAG,"connentSocket");
		if (listData == null) {
			listData = new ArrayList<Socket>();
		}
			sockerClient = new SocketUtils(socket);
			// 从服务器返回的数据
			sockerClient.setMessageListener(new SocketListener() {
				@Override
				public void stream(final String hostAddress) {
					try {
						runOnUiThread(new Runnable() {
							public void run() {
								tvMessge.append(hostAddress + "\n");
							}
						});
						Log.e(TAG, "客户端收到信息  " + hostAddress);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});

		sockerClient.send(Const.system_name);
		sockerClient.send(Const.system_version);
	}

	private String getServerInfo(String msg) {
		String message = msg;
		if (msg.startsWith("#")) {
			return msg;
		}
		if (Const.system_name.equals(msg)) {
			return SystemUtil.getDeviceBrand();
		}
		if (Const.system_version.equals(msg)) {
			return SystemUtil.getSystemVersion();
		}
		if (msg.equals("fileList")) {
			return getFileList();
		}
		if (msg.startsWith("sendFile:")) {
			boolean openFileServer = openFileServer();
			if (openFileServer) {//打开保存服务成功
				Log.e(TAG, "开启文件接收成功");
				return "openSaveServer:success";
			} else {
				Log.e(TAG, "开启文件接收失败");
				return "openSaveServer:error";
			}
		}

		return message;
	}

	/**
	 * 打开接收文件的服务
	 */
	private boolean openFileServer() {
		Log.e(TAG, "文件接收服务");
		if(fileServer!=null) {
			Log.e(TAG, "上次保存未结束");
			return false;
		}
		try {
			fileServer = new FileServer(send_file_port, savePath);
			fileServer.setFileSaveListener(new FileSaveListener() {
				@Override
				public void save(long pos, long size) {
//					Log.e(TAG, "保存进度：" + pos + "  size:" + size);
				}

				@Override
				public void onSuccess() {
					fileServer.close();
					fileServer = null;
					sockerServer.send("saveFile:success");
					Log.e(TAG, "保存进度 完成");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvMessge.append("文件接收完成");
						}
					});
				}

				@Override
				public void start(long size) {
					Log.e(TAG, "开始保存  size "+size );
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvMessge.append("开始接收文件"+"\n");
						}
					});
				}
			});
			new Thread() {
				public void run() {
					try {
						fileServer.task();
					} catch (Exception e) {
						fileServer.close();
						fileServer = null;
						e.printStackTrace();
					}
				};
			}.start();
			return true;
		} catch (Exception e) {
			if(fileServer!=null) {
				fileServer.close();
			}
			fileServer = null;
			e.printStackTrace();
			return false;
		}
	}

	class MyAdapter extends BaseAdapter {
		private List<Socket> list;

		public void setData(List<Socket> list) {
			this.list = list;
		}

		@Override
		public int getCount() {
			return list == null ? 0 : list.size();
		}

		@Override
		public Object getItem(int position) {

			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = View.inflate(parent.getContext(), R.layout.adapter_item, null);
			TextView tv = (TextView) view.findViewById(R.id.tv_info);
			tv.setTextColor(Color.RED);
			String string = list.get(position).getInetAddress().getHostAddress();
			tv.setText(string);
			return view;
		}

	}

	@Override
	protected void onDestroy() {
		if (fileServer != null) {
			fileServer.close();
		}
		if (sockerServer != null) {
			sockerServer.close();
		}
		if (sockerClient != null) {
			sockerClient.close();
		}
		super.onDestroy();
	}

}
