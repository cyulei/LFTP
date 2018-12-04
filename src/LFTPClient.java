import java.io.IOException;

import org.apache.commons.cli.*;  

public class LFTPClient
{
	static Options opts = new Options();  
	
    static{  
        // -h --help 帮助文档  
        // -f --file file参数  
    	// -m --mode 上传还是下载
    	// -s --server 服务器地址
        opts.addOption("h", "help", false,"The command help");  
        opts.addOption("f", "file", true,"Input your file path.For example: E:\\\\dcp\\\\1.txt");  
        opts.addOption("m", "mode", true,"Input you want to upload or download.For example: lsend or lget");  
        opts.addOption("s", "server", true,"Input the server ip address.For example: 127.0.0.1");  
        opts.addOption("o", "output", true,"Input the output filename path.For example: output.txt"); 
    }  
    /** 
     * 提供程序的帮助文档 
     */  
    static void printHelp(Options opts) {  
        HelpFormatter hf = new HelpFormatter();  
        hf.printHelp("Show how to use cli.", opts);  
    }  
    
	public static void main(String[] args) throws IOException, InterruptedException 
	{
		
        // 解析参数  
        CommandLineParser parser = new PosixParser();  
        CommandLine cl = null;
		try {
			cl = parser.parse(opts, args);
		} catch (ParseException e) {
       	 	System.out.println("参数不正确");
			e.printStackTrace();
		}  
          
        if(cl.hasOption("h")) 
        {  
            printHelp(opts);  
            return;  
        }  
        if(cl.hasOption("f") && cl.hasOption("m") && cl.hasOption("s") && cl.hasOption("o"))
        {
            String filename = cl.getOptionValue("f");  
            String mode =  cl.getOptionValue("m");  
            String severaddress = cl.getOptionValue("s");  
            String outputfilename = cl.getOptionValue("o"); 
    		UDPClient client = new UDPClient();
            if(mode.equals("lsend"))
            {

        		/*
        		//确定客户端的服务器地址和端口以及文件
        		client.BindClientPort("127.0.0.1",8080,"1.txt");
        		*/
				client.BindClientPort(severaddress,8080,filename,false,outputfilename);
            }
            else if(mode.equals("lget"))
            {
				client.BindClientPort(severaddress,8080,filename,true,outputfilename);
            }
            else
            {
           	 	System.out.println("模式不正确,-h查看命令使用");
            }
        }
        else
        {
       	 	System.out.println("参数不正确,-h查看命令使用");
        }
	}
}
