import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TCPReceiver {
	FileOutputStream TCPReceiverLog=null;				 //日志
	//--------通用---------------------------------------------------------------
    private String netAddress = "";              //发送到的地址
    private int PORT = 0;                        //发送到的端口
	private DatagramSocket datagramSocket;       //数据报套接字
	public AClock clock = new AClock();          //定时器

	int realDataLength = 1000;                   //真实数据的长度
	private int expectSeq = 0;		//所期望收到的报文序号
	private packer onepacker;       //需要一个将收到的包打包的类
	
	public int testtt=0;
	private int MAX_RECEIVE_DATA = realDataLength+32;            //最大接收数据报文的字节长度
	private byte[] receData = new byte[MAX_RECEIVE_DATA];       //接收到的数据报文 
	
	//服务器的端口
	int serverPort = 8080;
	
	//即将关闭状态
	public boolean goodbye = false;
	//已经关闭状态
	public boolean close = false;
	
	//------------流量控制机制--------------------
	private int receBuffer = MAX_RECEIVE_DATA*10;               //接收缓存大小
	//RcvWindow（接收方的空闲空间） = RcvBuffer（接收缓存为）10080字节
	private int receWindow = receBuffer;
	//缓存中的数据大小 = 0字节
	private int dataInBuffer = 0;

	private boolean isServer;   //判断是不是服务器，如果是服务器就不用新建接收缓存
	
	//初始化
	public void InitializeTCPReceiver(DatagramSocket ds,String address,int port,int serverPort,String outputfilename,boolean isSer)
	{
		try {
			TCPReceiverLog = new FileOutputStream("TCPReceiverLog.txt");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		isServer = isSer;
		datagramSocket = ds;
		netAddress = address;
		PORT = port;
   	 	//System.out.println("输出文件名大小:"+outputfilename.length()+"输出文件名:"+outputfilename);
		onepacker = new packer(outputfilename);
		this.serverPort = serverPort;
	}
	public void SendACK(int ack)
	{
    	//将传入的ack的序号和接收窗口大小拼接为一个byte[]的包
		byte[] temp = SetAckPackage(ack);
		try
		{
			//将报文发送到指定目的地
	        InetAddress add = InetAddress.getByName(netAddress);
	        DatagramPacket datagramPacket = new DatagramPacket(temp, temp.length, add, PORT);
	        datagramSocket.send(datagramPacket);		
			 
	        WriteLog("Send an ack Message of :"+ack);
		}
		catch (UnknownHostException e) 
		{
            e.printStackTrace();
        }  
		catch (IOException e) 
		{
            e.printStackTrace();
        } 		
	}
	private byte[] SetAckPackage(int acknumber)
	{
		byte[] fill=new byte[1];
		byte[] ack=Formatter.getFormat(acknumber, 0, 0, 0, receWindow, 0, 0, 0, fill) ;
		System.out.println("接收窗口大小为:"+receWindow);
		return ack;
	}

	public void ReceiveData() throws InterruptedException {

        try {
        	DatagramPacket receiveData = new DatagramPacket(receData, receData.length);
			datagramSocket.receive(receiveData);
	        netAddress = receiveData.getAddress().toString();
	        netAddress = netAddress.substring(1);
	        PORT = receiveData.getPort();

	        int fin = Formatter.getFIN(receiveData.getData());
	        //WriteLog("receive message from port :" + PORT + "address from:"+netAddress);
	        if(fin == 1)
	        {
	        	byte[] data = new byte[1000];
	        	byte[] formatData = Formatter.getFormat(1,0,0,0,0,0,0,0,data);
				//将报文发送到指定目的地
		        InetAddress add = InetAddress.getByName(netAddress);
	            DatagramPacket datagramPacket = new DatagramPacket(formatData, formatData.length, add, PORT);
	            try {
	    			datagramSocket.send(datagramPacket);
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}	
		   	 	System.out.println("接收方发送ACK");
             	System.out.println("发送到"+add+"端口:"+PORT);
	            formatData = Formatter.getFormat(0,0,1,0,0,0,0,0,data);
	            datagramPacket = new DatagramPacket(formatData, formatData.length, add, PORT);
	            try {
	    			datagramSocket.send(datagramPacket);
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		}	
		   	 	System.out.println("接收方发送FIN");
	            goodbye = true;
	            //计时器计时，到时间关闭
	            clock.StartFINClock(2000,this,null,0,null);
	            return;
	        }
	        int ack = Formatter.getACK(receiveData.getData());

	        if(goodbye && ack == 1)
	        {
	        	UDPServer.BreakFromClient(serverPort);
	        	close = true;
	        	return;
	        }
	        
	        int blank1 = Formatter.getblanket1(receiveData.getData());
	        int blank2 = Formatter.getblanket2(receiveData.getData());
	        //接收到一个包是表示接收缓存已满的
	        if(blank1 == 1 && blank2 == 1)
	        {
	        	//缓存大小不改变，且不解析数据，直接返回ACK信息以及RcvWindow值
	        	SendACK(expectSeq);	
	    		return;
	        }
	        
	        if(!isServer)
	        {
		        //缓存中的数据大小 =缓存中的数据大小+1个包大小
		        dataInBuffer = dataInBuffer + MAX_RECEIVE_DATA;
		        //RcvWindow = RcvBuffer-缓存中的数据大小
		        receWindow = receBuffer - dataInBuffer;
	        }
	        else
	        {
	        	UDPServer.setServerDataInBuffer(MAX_RECEIVE_DATA);
	        	receWindow = UDPServer.getServerRecWindow();
	        }

	        
	        
	        byte[] data = receiveData.getData();
	        //if(Formatter.getSEQ(data)==3&&testtt==0) {testtt=1;return;}//手动丢掉第三个包
	        //if(packetBuffer.byteArrayToInt(data, 0)==4)return;//手动丢掉第三个包
	        
	        onepacker.pack(data);//！！！！！！注意此处逻辑

	        WriteLog("receive packet with seq:"+ Formatter.getSEQ(data));

	        expectSeq=onepacker.getMaxSeq()+1;//当前已经写入的最大包号+1
	        /*int seqInt = packetBuffer.byteArrayToInt(data,0);
	        if(seqInt==expectSeq+1) {
		        
	        	expectSeq=seqInt+1;//更新预期包号,使其为当前确认过的包+1
	        }*/
	        SendACK(expectSeq);
	        
	        if(!isServer)
	        {
		        //提取完数据后数据在缓存中大小减少
		        dataInBuffer = dataInBuffer - MAX_RECEIVE_DATA;
		        receWindow = receBuffer - dataInBuffer;
	        }
	        else
	        {
	        	UDPServer.setServerDataInBuffer(-MAX_RECEIVE_DATA);
	        	receWindow = UDPServer.getServerRecWindow();
	        }

		} catch (IOException e) {
			e.printStackTrace();
		}
 
	}
	public void Close()
	{
    	UDPServer.BreakFromClient(serverPort);
    	close = true;
	}
	
	public void WriteLog(String toWrite) {
		//log.println(toWrite+'\n');
		try {
			TCPReceiverLog.write(toWrite.getBytes("utf-8"));
			TCPReceiverLog.write("\n".getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TCPLog.write("\n".getBytes("UTF-8"));
	}
}
