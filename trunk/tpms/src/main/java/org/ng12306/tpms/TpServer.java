package org.ng12306.tpms;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;

import org.jboss.netty.handler.codec.serialization.ClassResolvers;

import org.ng12306.tpms.runtime.TicketQueryArgs;
import org.ng12306.tpms.runtime.TestRailwayRepository;
import org.ng12306.tpms.runtime.TestTicketPoolManager;
import org.ng12306.tpms.runtime.ServiceManager;

// ITpServer��Ĭ��ʵ�֣����Ҫ��A/B���ԵĻ���Ӧ����
// ��TpServer�̳�ʵ�����ַ�ʽ
public class TpServer implements ITpServer {
     private int _port;
     private ChannelGroup _channels;
     private ChannelFactory _factory;
     private boolean _started;
     
     public TpServer(int port){
	  _port = port;
	  _channels = new DefaultChannelGroup("ticket-pool");
     }
     
     public void start() {
	  // ����Ĵ��붼�ǿ���ֱ����Netty�Ĺ����￴���ģ�����ϸע��
	  _factory = new NioServerSocketChannelFactory(
	       // TODO: ��Ҫд���ܲ�����������֤cached thread pool�Ƿ��ã�
	       Executors.newCachedThreadPool(),
	       Executors.newCachedThreadPool());
	  ServerBootstrap bootstrap = new ServerBootstrap(_factory);
	  bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
		    public ChannelPipeline getPipeline() throws Exception {
			 // ������Ƿ�����Ϣ����Handlerջ - ��Ȼ���ֽйܵ���
			 return Channels.pipeline(
			      new ObjectEncoder(),
			      new ObjectDecoder(
				   ClassResolvers.cacheDisabled(
					getClass().getClassLoader())),
			      new QueryTrainServerHandler());
		    }
	       });
	  _channels.add(bootstrap.bind(new InetSocketAddress(_port)));
	  try { 
	       // ���eventbus��Ϊ��־\������ϵ���ϱ��ݷ��������޷�����
	       // ��ô�͹ر�Netty������
	       EventBus.start();
	       registerService();
	       _started = true;	       
	  } catch ( Exception e ) {
	       stopNettyServer();
	  }
     }

     public void stop() {
	  if ( _started ) {
	       try { 
		    // EventBus���û�������ر�,�����־����
		    EventBus.shutdown();
	       } catch ( Exception e ) {
		    // TODO: ��¼EventBus�޷������رյ���־
	       }

	       stopNettyServer();
	  }
     }

     // ���ݳ��ӵĴ���,���еķ�����ҪԤ��ע��,Ȼ����ʹ��ʱ,ͨ��getRequiredService
     // ��ȡ,����Ioc,��˷�����������ʱ,��Ҫע����Щ����
     private void registerService() throws Exception {
	  ServiceManager
	       .getServices()
	       .initializeServices(new Object[] {
			 new TestRailwayRepository(), 
			 new TestTicketPoolManager()});
     }

     private void stopNettyServer()  {
	  ChannelGroupFuture future = _channels.close();
	  future.awaitUninterruptibly();
	  _factory.releaseExternalResources();
	  _started = false;
     }

     class QueryTrainServerHandler extends SimpleChannelUpstreamHandler {
	  @Override
	  public void messageReceived(ChannelHandlerContext ctx,
				      MessageEvent e) {
	       // Ʊ�ط����������첽����io�ķ�ʽ������Ϣ
	       // ��Ϊ���ǵ�Handler�Ǵ�SimpleChannelUpstreamHandler�̳�������
	       // Netty������ǽ������ɢ�����ݰ�����һ��������ԭʼ�Ŀͻ����������ݰ�
	       // ���⣬��������֮ǰ�����Ѿ������������������Handler�ˣ����Կ���
	       // ֱ��ͨ��e.getMessage()��ȡ�ͻ��˷��͵Ķ���
	       TicketQueryArgs event = (TicketQueryArgs)e.getMessage();
	       // ���ݸ�disruptor���ֶ��н��д���
	       Channel channel = e.getChannel();
	       EventBus.publishQueryEvent(event);       
	  }

	  // TODO: ��Ҫ�����ʵ�ַ����쳣����־��ʽ
     }
}
