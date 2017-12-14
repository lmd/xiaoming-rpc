/*
 * Copyright (c) 2017 xiaoming software Technology Co., Ltd.
 * All Rights Reserved.
 */
package com.xiaoming.software.rpc.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.xiaoming.software.rpc.client.XiaoMingRpcClient;
import com.xiaoming.software.rpc.exception.XiaoMingRpcException;
import com.xiaoming.software.rpc.server.handler.DefaultServerHandlerImpl;
import com.xiaoming.software.rpc.server.handler.XiaoMingRpcServerHandler;


/**
 * rpc server
 * @author xiaoming
 */
public class XiaoMingRpcServer {
	private ExecutorUtil executorUtil = new ExecutorUtil();
	private int port = 18088;
	private int maxClientNum = 0;
	
	private XiaoMingRpcServerHandler handler = null;
	
	public XiaoMingRpcServer(int port, XiaoMingRpcServerHandler handler, int maxClientNum){
		this.port = port;
		this.handler = handler;
		this.maxClientNum = maxClientNum;
		executorUtil.setMaxPoolSize(maxClientNum);
		executorUtil.initExecutor();
	}
	/**
	 * To start server
	 */
	public void startServer(){
		ServerSocket server = null;
		try {
			server = new ServerSocket(this.port);
			System.out.println("Started xiaoming-software rpc server in port:" + this.port + "");
			while(true){
				Socket socket = server.accept();
				
				//开启新线程去执行请求
				if(executorUtil.getKeepAliveNum() <= 0){
					throw new XiaoMingRpcException("The task is over flow max pool size:" + maxClientNum);
				}
				executorUtil.executor( getTask( socket));
			}
		} catch (IOException e) {
			if(server != null){
				try {
					server.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		}
	}
	int i = 0;
	private Runnable getTask(Socket socket){
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					InputStream in = socket.getInputStream();
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					while(true){
		                String line = br.readLine();
		                if (line != null) {
		                	//调用业务handler
							handler.handle(line);
		                }
					}
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					if(socket != null){
						//关闭socket
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					
				}
			}
		};
		return task;
	}
	
	public static void main(String[] args) {
		DefaultServerHandlerImpl handler = new DefaultServerHandlerImpl();
		XiaoMingRpcServer  server = new XiaoMingRpcServer(8080, handler, 128);
		server.startServer();
	}
	
}

/**
 * 线程池配置
 * @author xiaoming
 * 2017年4月20日
 */
class ExecutorUtil {  
  
    /** 最大线程数 */  
    private int maxPoolSize = 128;
    
    private ThreadPoolExecutor executor = null;
    
    public void setMaxPoolSize(int maxPoolSize){
    	this.maxPoolSize = maxPoolSize;
    }
    
    public int getKeepAliveNum(){
    	return executor.getMaximumPoolSize() - executor.getActiveCount();
    }
    
	public void initExecutor() {
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxPoolSize);
		this.executor = executor;
	} 
	/**
	 * 异步执行
	 * @param task
	 * @author xiaoming
	 */
	public void executor(Runnable task){
		executor.execute(task);
	}
}  
