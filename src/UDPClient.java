import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {

	FileOutputStream TCPLog=null;				 //日志
	
	private String netAddress = "127.0.0.1";     //服务器的地址
    private int PORT = 8080;                     //服务器的端口
   
    InetAddress add;                             //服务器地址
    
    private DatagramSocket datagramSocket;       //数据报套接字
    byte[] revData = new byte[1032];             //接收连接信息的数据报大小
    DatagramPacket receiveData = new DatagramPacket(revData, revData.length);

	private AClock clock = new AClock();          //定时器
	
    public void BindClientPort(String netaddress,int port,String filename,boolean download,String outputfilename) throws InterruptedException, IOException
	{

		TCPLog=new FileOutputStream("UDPClientLog.txt");

		//得到服务器地址和端口
		netAddress = (netaddress == "")?netAddress:netaddress;
		PORT = (port == 0)?PORT:port;
		add = InetAddress.getByName(netAddress);
		WriteLog("netAddress :" + netAddress);
        WriteLog("port :" + PORT);
        //创建套接字，并不需要绑定到指定端口
        datagramSocket = new DatagramSocket();   		
        
    	//三次握手
    	int serverPort = HandshakeWithServer(download,outputfilename,filename);
    	
    	if(!download)
    	{
    		//发送文件
        	WriteLog("___________________________________________________");
        	TCPSender tcpSender=new TCPSender();
        	tcpSender.InitializeTCPSender(datagramSocket, netAddress, serverPort,filename);
        	tcpSender.StartSendData();

        	//主动发起四次挥手
        	SayGoodbyeWithServer(serverPort);
    	}
    	else
    	{
    		//接收从服务器传过来的文件
        	TCPReceiver tcpReceiver = new TCPReceiver();
        	//作为接收数据方，地址和端口可以先不配
        	tcpReceiver.InitializeTCPReceiver(datagramSocket, "", 0,serverPort,outputfilename,false);
        	//接收数据方会被动接收四次挥手
        	while(!tcpReceiver.close)
        	{
        		tcpReceiver.ReceiveData();
        	}
        	//定时关闭
        	Thread.sleep(2000);
        	CloseSocket();
    	}
    	System.out.println("程序执行结束关闭");
    }  
    
    //发送握手报文
    public void SendData(byte[] data)
    {
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, add, PORT);
        try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
    }
	//三次握手，和服务器进行连接
	public int HandshakeWithServer(boolean download,String outputfilename,String filename) throws InterruptedException
	{

        //第一次握手
    	//先只发1字节测试（应该发送SYN=1,SEQ=Clent_isn）
    	byte[] data = null;
    	if(!download)
    	{
    		data = outputfilename.getBytes();
	    	byte[] formatData = Formatter.getFormat(0,1,0,0,0,1,0,0,data);
	    	SendData(formatData);	
    	}
    	else
    	{
    		//下载的时候把要从服务器上下载的文件名发送过去
    		data = filename.getBytes();
	    	byte[] formatData = Formatter.getFormat(0,1,0,0,0,0,0,0,data);
	    	SendData(formatData);	
    	}
   	 	System.out.println("发送第一次握手报文");
        //开启定时器
    	clock.StartClockForLink(5, data, this, null);
    	
        //第二次握手
        //接收到SYN=1,SEQ=server_isn,ack=client_isn+1
        try {
			datagramSocket.receive(receiveData);
	   	 	System.out.println("接收到第二次握手报文");
		} catch (IOException e) {
			e.printStackTrace();
		}
        //关闭定时器
        clock.CloseClock();
        
        int serverPort = Formatter.getblanket2(receiveData.getData());
        
        //第三次握手
    	//再发送SYN=0,SEQ=client_isn+1,ack=server_isn+1
    	byte[] tempData = Formatter.getFormat(1,0,0,1,0,1,0,0,data);
    	SendData(tempData);	
   	 	System.out.println("发送第三次握手报文");
   	 	
   	 	return serverPort;
	}
	//四次挥手，和服务器断开连接
    public void SayGoodbyeWithServer(int serverPort) throws InterruptedException, IOException
    {
    	//发送FIN=1
    	byte[] data = new byte[1000];
    	byte[] formatData = Formatter.getFormat(0,0,1,0,0,0,0,0,data);
        DatagramPacket datagramPacket = new DatagramPacket(formatData, formatData.length, add, serverPort);
        try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
    	System.out.println("客户端第一次挥手");
		
     	//定时器，时间到直接关闭套接字
    	clock.StartFINClock(2000, null,null,0,this);
    	
    	//接收ACK
		datagramSocket.receive(receiveData);
		System.out.println("第二次挥手接收到服务端的ACK");
    	
    	//接收FIN=1
		datagramSocket.receive(receiveData);
		System.out.println("接收到客户端第三次挥手");
		
    	//发送ACK
    	formatData = Formatter.getFormat(1,0,0,0,0,0,1,0,data);
    	datagramPacket = new DatagramPacket(formatData, formatData.length, add, serverPort);
        try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		System.out.println("发送第四次挥手ACK");
		
    	//定时关闭
    	Thread.sleep(2000);
    	CloseSocket();
    }
    //关闭套接字
    public void CloseSocket()
    {
        //关闭套接字
        if (datagramSocket != null) 
        {
            datagramSocket.close();
        }   
        System.out.println("客户端套接字关闭");
    }
	public void WriteLog(String toWrite) {
		//log.println(toWrite+'\n');
		try {
			TCPLog.write(toWrite.getBytes("utf-8"));
			TCPLog.write("\n".getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TCPLog.write("\n".getBytes("UTF-8"));
	}
    
    
}
