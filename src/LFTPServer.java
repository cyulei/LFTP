import java.io.IOException;

public class LFTPServer {
	public static void main(String[] args) throws IOException 
	{
		UDPServer server = new UDPServer();
		//绑定端口
		server.BindServerPort(8080);
		//监听有没有客户端连接
		server.PublicPortReceiveData();
		//关闭套接字
		server.CloseSocket();
	}
}
