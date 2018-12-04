import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServer {

    private int PORT = 8080;                             //默认端口号
    private DatagramSocket datagramSocket;               //数据报套接字
    public TCPReceiver TCPReceiver=null;
    
    //-------------------------------------------------------------------
    byte[] revData = new byte[1032];             //接收连接信息的数据报大小
    DatagramPacket receiveData = new DatagramPacket(revData, revData.length);

    int index = 1;
    
    //当前进行握手的客户端的端口和地址
    int curClientPort;
    InetAddress curClientAddress = null;
    DatagramSocket curentDS = null;
    
    static Map<Integer,DatagramSocket> port2SocketMap = new HashMap<Integer,DatagramSocket>();       //一个端口号对应一个套接字
	private ExecutorService executor = Executors.newCachedThreadPool() ;  //线程池
	
	private AClock clock = new AClock();          //定时器
	
	boolean isStartServer = true;
	private UDPServer udpserver;
	
	//------------流量控制机制--------------------
	private static int receBuffer = 10320;               //接收缓存大小
	//RcvWindow（接收方的空闲空间） = RcvBuffer（接收缓存为）
	private static int receWindow = receBuffer;
	//缓存中的数据大小 = 0字节
	private static int dataInBuffer = 0;
	
	public static int getServerRecWindow() {
		return receWindow;
	}
	public static void setServerDataInBuffer(int db) {
		dataInBuffer = dataInBuffer + db;
		receWindow = receBuffer - dataInBuffer;
	}
	
    //绑定服务器的端口
    public void BindServerPort(int port)
    {
        try 
        {
            //创建一个数据报套接字绑定到指定端口
        	PORT = port == 0?PORT:port;
        	//System.out.println("port :" + PORT);
            datagramSocket = new DatagramSocket(PORT);
            udpserver = this;
        } 
        catch (SocketException e) 
        {
            e.printStackTrace();
        }
    }
    //开始接收数据
    public void StartReceiveData(DatagramSocket ds,int port,String outputfilename) throws InterruptedException
    {
    	//创建TCP
    	TCPReceiver tcpReceiver = new TCPReceiver();
    	//作为接收数据方，地址和端口可以先不配
    	tcpReceiver.InitializeTCPReceiver(ds, "", 0,port,outputfilename,true);
    	while(true)
    	{
        	//根据port找到套接字并且关闭
        	if(port2SocketMap.containsKey(port))
        	{
        		tcpReceiver.ReceiveData();
        	}
        	else
        	{
        		break;
        	}
    	}
    }
    

    //发送握手报文
    public void SendData(byte[] data)
    {
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, curClientAddress, curClientPort);
        try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
    }
    public void HandshakeWithServer(int sign,String filename,String clientAddress,int clientPort) throws InterruptedException, IOException
    {

   	 	//配置握手的客户端地址和端口
   	    String netAddress = receiveData.getAddress().toString();
        netAddress = netAddress.substring(1);
		curClientAddress = InetAddress.getByName(netAddress);
		curClientPort = receiveData.getPort();
		
        //允许分配一个端口
        int newPort = PORT+index;
        index++;
        
        //第二次握手
    	//把分配的端口号发送过去
    	byte[] data = new byte[1000];
    	byte[] formatData = Formatter.getFormat(1,1,0,0,0,0,newPort,0,data);
    	SendData(formatData);	
   	 	System.out.println("发送第二次握手报文");
        //开启定时器
    	clock.StartClockForLink(5, data, null,udpserver);
    	
    	//接收第三次握手报文
		datagramSocket.receive(receiveData);
   	 	System.out.println("接收到第三次握手报文");
        //关闭定时器
        clock.CloseClock();
        
        //建立一个新的套接字
        DatagramSocket newDS = new DatagramSocket(newPort);
        curentDS = newDS;
        //加入map中
        port2SocketMap.put(newPort, newDS);
   	 	System.out.println(newPort+"开启");
        if(sign == 1) //接收数据
        {
            executor.submit(new Runnable(){public void run(){try {
				StartReceiveData(curentDS,newPort,filename);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}}});
        }
        else if(sign == 0)
        {
        	//发送数据
        	 executor.submit(new Runnable(){public void run(){

             	TCPSender tcpSender=new TCPSender();
             	//新的套接字，接收方地址，接收方端口，发送的文件
             	System.out.println("发送到"+clientAddress+"端口:"+clientPort);
             	tcpSender.InitializeTCPSender(newDS,clientAddress,clientPort,filename);
             	tcpSender.StartSendData();

             	//发起四次挥手
             	try {
					SayGoodbyeWithClient(clientPort,clientAddress,newDS,newPort);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	 
        	 }});
        }
    }
    
	//四次挥手，和客户端
    public void SayGoodbyeWithClient(int clientPort,String clientAddress,DatagramSocket ds,int newPort) throws InterruptedException, IOException
    {
    	//发送FIN=1
    	byte[] data = new byte[1000];
    	byte[] formatData = Formatter.getFormat(0,0,1,0,0,0,0,0,data);
        DatagramPacket datagramPacket = new DatagramPacket(formatData, formatData.length, InetAddress.getByName(clientAddress), clientPort);
        try {
        	ds.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
    	System.out.println("服务器第一次挥手");
     	System.out.println("发送到"+clientAddress+"端口:"+clientPort);
     	
     	//定时器，时间到直接关闭套接字
     	clock.StartFINClock(2000, null,udpserver,newPort,null);
     	
    	//接收ACK
    	ds.receive(receiveData);
		System.out.println("第二次挥手接收到客户端的ACK");
    	
    	//接收FIN=1
		ds.receive(receiveData);
		System.out.println("接收到服务器第三次挥手");
		
    	//发送ACK
    	formatData = Formatter.getFormat(1,0,0,0,0,0,1,0,data);
    	datagramPacket = new DatagramPacket(formatData, formatData.length, InetAddress.getByName(clientAddress), clientPort);
        try {
        	ds.send(datagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}	
		System.out.println("发送第四次挥手ACK");
		BreakFromClient(newPort);
    }
    
    //断开和一个客户端的连接
    public static void BreakFromClient(int port)
    {
   	 	System.out.println(port+"关闭");
    	//根据port找到套接字并且关闭
    	if(port2SocketMap.containsKey(port))
    	{
    		DatagramSocket d = port2SocketMap.get(port);
	   	 	System.out.println("服务器端口: "+port+"关闭");
    		d.close();
    		
    		//删除该键值对
            Iterator<Integer> iterator = port2SocketMap.keySet().iterator();
            while (iterator.hasNext())
            {
            	Integer key = iterator.next();
                if(key == port)
                {
                    iterator.remove();
                    break;
                }
            }
    	}
    }
    //监听
    public void PublicPortReceiveData()
    {
    	while(true)
    	{
    		if(isStartServer)
    		{
    			isStartServer = false;
    			executor.submit(new Runnable(){public void run(){
            		while(true)
            		{
            	        try {
            				datagramSocket.receive(receiveData);
            		   	 	System.out.println("服务器总端口接收到一个报文");
            		  	 	
            		   	 	int syn = Formatter.getSYN(receiveData.getData());
            		   	 	
            		   	 	int sign =  Formatter.getblanket1(receiveData.getData());  //1代表上传数据，0代表下载数据
            		   	 	byte[] namebyte = Formatter.getData(receiveData.getData());
            		   	 	String filename = byteToStr(namebyte);
            		   	 	if(syn == 1)
            		   	 	{
            			        String clientAddress = receiveData.getAddress().toString();
            			        clientAddress = clientAddress.substring(1);
            			        int clientPort = receiveData.getPort();
            		   	 		HandshakeWithServer(sign,filename,clientAddress,clientPort);
            		   	 	}
        		   	 	
            			} catch (IOException e) {
            				e.printStackTrace();
            			} catch (InterruptedException e) {
							e.printStackTrace();
						}
            		}
        		}});	
    		}
    	}
    }
    //关闭套接字
    public void CloseSocket()
    {
        //关闭套接字
        if (datagramSocket != null) 
        {
            datagramSocket.close();
        }   	
    }
    //byte转为string
	public String byteToStr(byte[] buffer) {
		try {
			int length = 0;
			for (int i = 0; i < buffer.length; ++i) {
				if (buffer[i] == 0) {
					length = i;
					break;
				}
			}
			return new String(buffer, 0, length, "UTF-8");
		} catch (Exception e) {
			return "";
		}
	}
}
