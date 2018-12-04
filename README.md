# LFTP
基于UDP实现TCP机制的应用，分为客户端与服务端两个部分，一个客户端可以支持多个服务端连接，进行上传或者下载工作

### 文件目录
```
│  commons-cli-1.4.jar            此项目所需要的外部jar包用于生成CLI的参数读取
│  README.md                       README
├─bin
│      LFTPClient.class            编译生成的客户端的主函数入口
│      LFTPServer.class            编译生成的服务端的主函数入口
└─src
        AClock.java                一个定时器类
        Formatter.java             报文格式的定义
        LFTPClient.java            客户端的主函数使用外部jar包对命令行参数进行解析，参数使用详情输入-h参数
        LFTPServer.java            服务端的主函数运行时候绑定本机的地址为服务器地址
        packer.java                将接收的报文打包的类
        packetBuffer.java          报文打包的缓存类
        slicer.java                将文件分成一个一个报文的类
        slicerBuffer.java          发送方分解报文缓存类
        TCPReceiver.java           实现了TCP接收方的机制
        TCPSender.java             实现了TCP发送方的机制
        UDPClient.java             一个客户端类
        UDPServer.java             一个服务端类
```