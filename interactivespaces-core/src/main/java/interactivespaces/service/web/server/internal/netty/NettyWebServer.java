/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.service.web.server.internal.netty;

import static org.jboss.netty.channel.Channels.pipeline;
import interactivespaces.InteractiveSpacesException;
import interactivespaces.service.web.server.HttpFileUploadListener;
import interactivespaces.service.web.server.WebServer;
import interactivespaces.service.web.server.WebServerWebSocketHandlerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

/**
 * A web server based on Netty.
 * 
 * @author Keith M. Hughes
 */
public class NettyWebServer implements WebServer {

	/**
	 * Name of the server.
	 */
	private String serverName;

	/**
	 * Port the server should run on.
	 */
	private int port;

	/**
	 * The server handler for requests.
	 */
	private NettyWebServerHandler serverHandler;

	/**
	 * Server handler for web sockets.
	 */

	private Channel serverChannel;

	/**
	 * All channels we know about in the server.
	 */
	private ChannelGroup allChannels;

	/**
	 * Factory for all channels coming into the server.
	 */
	private NioServerSocketChannelFactory channelFactory;

	private ScheduledExecutorService bossThreadPool;

	private ScheduledExecutorService workerThreadPool;

	/**
	 * Logger for the web server.
	 */
	private Log log;

	public NettyWebServer(String serverName, int port,
			ScheduledExecutorService threadPool, Log log) {
		this(serverName, port, threadPool, threadPool, log);
	}

	public NettyWebServer(String serverName, int port,
			ScheduledExecutorService bossThreadPool,
			ScheduledExecutorService workerThreadPool, Log log) {
		this.serverName = serverName;
		this.port = port;
		this.bossThreadPool = bossThreadPool;
		this.workerThreadPool = workerThreadPool;
		this.log = log;

		serverHandler = new NettyWebServerHandler(this);
	}

	@Override
	public void startup() {

		allChannels = new DefaultChannelGroup(serverName);

		channelFactory = new NioServerSocketChannelFactory(bossThreadPool,
				workerThreadPool);

		ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
		
		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				// Create a default pipeline implementation.
				ChannelPipeline pipeline = pipeline();
				pipeline.addLast("decoder", new HttpRequestDecoder());
				// pipeline.addLast("aggregator", new
				// HttpChunkAggregator(4615604));
				pipeline.addLast("encoder", new HttpResponseEncoder());
				pipeline.addLast("handler", serverHandler);

				return pipeline;
			}
		});

		serverChannel = bootstrap.bind(new InetSocketAddress(port));
		allChannels.add(serverChannel);
	}

	@Override
	public void shutdown() {
		if (allChannels != null) {
			ChannelGroupFuture future = allChannels.close();
			future.awaitUninterruptibly();
			
			channelFactory = null;
			allChannels = null;
		}
	}

	@Override
	public void addStaticContentHandler(String uriPrefix, File baseDir) {
		if (!baseDir.exists())
			throw new InteractiveSpacesException(String.format(
					"Cannot find web folder %s", baseDir.getAbsolutePath()));

		serverHandler.addHttpContentHandler(new NettyStaticContentHandler(
				serverHandler, uriPrefix, baseDir));
	}

	@Override
	public void setWebSocketHandlerFactory(String webSocketUriPrefix,
			WebServerWebSocketHandlerFactory webSocketHandlerFactory) {
		serverHandler.setWebSocketHandlerFactory(webSocketUriPrefix,
				webSocketHandlerFactory);
	}

	@Override
	public void setHttpFileUploadListener(HttpFileUploadListener listener) {
		serverHandler.setHttpFileUploadListener(listener);
	}

	@Override
	public String getServerName() {
		return serverName;
	}

	@Override
	public int getPort() {
		return port;
	}

	/**
	 * Get the worker thread pool.
	 * 
	 * @return
	 */
	public ExecutorService getWorkerThreadPool() {
		return workerThreadPool;
	}

	/**
	 * A new channel was opened. Register it so it can be properly shut down.
	 * 
	 * @param channel
	 */
	public void channelOpened(Channel channel) {
		allChannels.add(channel);
	}

	/**
	 * Get the web server's logger.
	 * 
	 * @return
	 */
	public Log getLog() {
		return log;
	}
}
