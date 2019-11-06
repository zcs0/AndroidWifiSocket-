# AndroidWifiSocket-
使用wifi实现手机之间的和文件传输

功能已实现，代码需自己封装代码，Socket通讯可使用到电脑端，属于通用型

文中使用两个Socket进行通讯，两Socket进行文件的传输，具体操作代码中有注释

private SocketUtils sockerServer;//本机的服务(通讯使用)

private SocketUtils sockerClient;//连接远程的服务(通讯使用)

private FileClient fileClient;//上传文件的服务

private FileServer fileServer;//接收文件的服务


文中使用两个Socket进行通讯，两Socket进行文件的传输，具体操作代码中有注释

private void scanDevice()//开始扫描同一网段下的IP，并测试是否有可用的拿到可以用的Socket

private void connentSocket(Socket socket);//开始连接远程手机的Socket获取设备的型号和Android版本

private String getServerInfo(String msg);//本方法是服务端调用的，用于辨别客户端请求的意图做出对应的操作

private boolean openFileServer();//打开一个接收文件的Socket服务

SocketUtils.setMessageListener();//设置会话的监听

private void sendFile(final String path) ;//发送一个文件

public SocketUtils(int port);//作为服务端

public SocketUtils(String host,int port);//作为客户端

