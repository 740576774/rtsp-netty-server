package com.zhuyun;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ResourceLeakDetector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.zhuyun.handler.HeartBeatServerHandler;
import com.zhuyun.handler.RtcpHandler;
import com.zhuyun.handler.RtpHandler;
import com.zhuyun.handler.RtspHandler;

/**
 *
 *
 */
public class RtspNettyServer {
    private static Bootstrap udpRtpstrap = new Bootstrap();
    private static Bootstrap udpRtcpstrap = new Bootstrap();
    public static Channel rtpChannel;
    public static Channel rtcpChannel;
    public static int ALL_IDLE_TIME;						//读和写的超时时间
    
    public static int rtpPort = 54000;
    public static int rtspPort = 554;
    public static String outputPath = null;
    
    public static void initUdp(EventLoopGroup group)
    {
    	udpRtpstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1024*1024*5)
                .option(ChannelOption.SO_RCVBUF, 1024*1024*5)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                        nioDatagramChannel.pipeline().addLast(new RtpHandler());
                    }
                })
                .option(ChannelOption.SO_BROADCAST, false);
    	
    	udpRtcpstrap.group(group)
		        .channel(NioDatagramChannel.class)
		        .option(ChannelOption.SO_SNDBUF, 1024*1024)
		        .handler(new ChannelInitializer<NioDatagramChannel>() {
		            @Override
		            protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
		                nioDatagramChannel.pipeline().addLast(new RtcpHandler());
		            }
		        })
		        .option(ChannelOption.SO_BROADCAST, false);
    }

    public static void createUdp(int port)
    {
        try
        {
            System.out.printf("start udp bind %d \n", port);
            rtpChannel = udpRtpstrap.bind(port).sync().channel();
            rtcpChannel = udpRtcpstrap.bind(port+1).sync().channel();
            
            System.out.printf("end udp bind %d \n", port);
        }
        catch (InterruptedException e)
        {
        }
    }

    public static void main(String[] args) {
    	try {
			Properties properties = new Properties();
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					"C:/Users/zhouyinfei/Desktop/SVN/dsp/my_newtonGW/trunk/src/watt/rtsp-netty-server/src/main/resources/rtsp-server.properties"));
			properties.load(bufferedReader);
			rtpPort = Integer.parseInt(properties.getProperty("rtp.port"));
			rtspPort = Integer.parseInt(properties.getProperty("rtsp.port"));
			outputPath = properties.getProperty("output.path");
			ALL_IDLE_TIME = Integer.parseInt(properties.getProperty("all.idle.time"));
		} catch (NumberFormatException | IOException e1) {
			e1.printStackTrace();
		}
    	
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        EventLoopGroup listenGrp = new NioEventLoopGroup(1);
        EventLoopGroup workGrp = new NioEventLoopGroup(4);
        initUdp(workGrp);
        createUdp(rtpPort);

        try {
            ServerBootstrap rtspstrap = new ServerBootstrap();
            rtspstrap.group(listenGrp, workGrp)
            		.channel(NioServerSocketChannel.class)
            		.option(ChannelOption.SO_BACKLOG, 1024)
            		.option(ChannelOption.SO_REUSEADDR, true)
            		.childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024 / 2, 64 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                            		.addLast(new IdleStateHandler(0, 0, ALL_IDLE_TIME, TimeUnit.SECONDS))//5秒内既没有读，也没有写，则关闭连接
                                    .addLast(new RtspDecoder())
                                    .addLast(new RtspEncoder())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new RtspHandler())
        							.addLast(new HeartBeatServerHandler());
                        }
                    });


            ChannelFuture rtspFuture = rtspstrap.bind(rtspPort).sync();

            System.out.println("Moriturus te saluto!!!");

            rtspFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            listenGrp.shutdownGracefully();
            workGrp.shutdownGracefully();
        }
    }
}