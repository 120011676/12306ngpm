package org.ng12306.tpms;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import org.junit.*;
import static org.junit.Assert.*;
import static org.ng12306.tpms.support.TestConstants.*;

import org.ng12306.tpms.runtime.*;
import org.ng12306.tpms.support.ObjectBsonEncoder;
import org.ng12306.tpms.support.ObjectBsonDecoder;
import org.ng12306.tpms.support.TestNettyServer;

public class NettyIntegrationTest {
     class TestQueryTrainServerHandler extends SimpleChannelUpstreamHandler {
	  @Override
	  public void messageReceived(ChannelHandlerContext ctx,
				      MessageEvent e) {
	       TicketQueryArgs event = (TicketQueryArgs)e.getMessage();
	       event.channel = e.getChannel();
	       EventBus.publishQueryEvent(event);
	  }
     }

     // �����ڲ�����������Ʊ�ط����ͳ��β�ѯ��Netty������
     class TestQueryTrainHandler extends SimpleChannelUpstreamHandler {
	  // Ҫ����������͵Ĳ�ѯ���ݰ� - �������κ�
	  private final TicketQueryArgs _event;
	  private Train[] _response;
	  public Train[] getResponse() { return _response; }
	 
	  public TestQueryTrainHandler(String trainId) {
	       _event = new TicketQueryArgs();
	       _event.setTrainNumber(trainId);
	       _event.setDate(new LocalDate());
	  }
	  
	  @Override
	  public void channelConnected(ChannelHandlerContext ctx,
				       ChannelStateEvent e) {
	       e.getChannel().write(_event);
	  }
	  
	  @Override
	  public void messageReceived(ChannelHandlerContext ctx,
				      MessageEvent e) {
	       _response = (Train[])e.getMessage();
	       e.getChannel().close();
	  }

	  @Override
	  public void exceptionCaught(ChannelHandlerContext ctx,
				      ExceptionEvent e) {
	       e.getCause().printStackTrace();
	       e.getChannel().close();
	  }
     }
     
     @Test
     public void ������ݳ��β�ѯ���() throws Exception {
	  // ����Netty�����������Ӧ��Ҫ�ŵ�setUp������
	  startTestServer();

	  try {
	       final TestQueryTrainHandler handler = 
		    new TestQueryTrainHandler("G101");

	       connectToServer(handler);

	       // �ȴ�һ����
	       Thread.sleep(1000);
	       
	       // ����֤
	       Train[] results = handler.getResponse();
	       Train result = results[0];
	       
	       assertEquals("G101", result.name);
	       assertEquals("������", result.departure);
	       assertEquals("�Ϻ�����", result.termination);
	       
	       // һ�����εķ���ʱ��Ӧ��ֻ��ʱ�䣬û�����ڡ�
	       assertEquals("07:00",
			    result.departureTime);
	       assertEquals("12:23",
			    result.arrivalTime);
	       
	       // TODO: ����������������,��Ϊ��û�г��εľ�����λ����.
	       // ��ҵ��������ķ������֮���������������������
	       assertEquals(2, result.availables.length);
	  } finally { 
	       stopTestServer();
	  }
     }
     
     // ��������ǲ����õķ����� - ��������Netty API��
     private TestNettyServer _server;
     private void startTestServer() throws Exception {
	  EventBus.start();
	  _server = new TestNettyServer(TP_SERVER_PORT,
					new TestQueryTrainServerHandler());
	  _server.start();
     }

     private void stopTestServer() throws Exception {
	  if ( _server != null ) {
	       EventBus.shutdown();
	       _server.stop();
	  }
     }

     @Test
     public void �ɳ��β�ѯ�������Ʊ�ط�����API() throws Exception {
	  // ����Netty�����������Ӧ��Ҫ�ŵ�setUp������
	  startRealServer();

	  try {
	       final TestQueryTrainHandler handler = 
		    new TestQueryTrainHandler("G101");
	       
	       // �ͻ��˵Ĺ������������������һ�����β�ѯBSON����
	       connectToServer(handler);
	        
	       // �ȴ�һ����
	       Thread.sleep(1000);
	       
	       // ����֤
	       Train[] results = handler.getResponse();
	       assertNotNull(results);
	       Train result = results[0];
	       
	       assertEquals("G101", result.name);
	       assertEquals("������", result.departure);
	       assertEquals("�Ϻ�����", result.termination);
	       
	       // һ�����εķ���ʱ��Ӧ��ֻ��ʱ�䣬û�����ڡ�
	       assertEquals("07:00",
			    result.departureTime);
	       assertEquals("12:23",
			    result.arrivalTime);
	       
	       // TODO: ����������������,��Ϊ��û�г��εľ�����λ����.
	       // ��ҵ��������ķ������֮���������������������
	       assertEquals(2, result.availables.length);
	  } finally { 
	       stopRealServer();
	  }
     }

     // �������������Ʊ�ط������ˣ�Ϊ�����غ���ľ���ʵ�֣�����һ���ӿ�ITpServer
     private ITpServer _itpServer;
     private void startRealServer() throws Exception {
	  // TODO: TpServerӦ����Ioc������
	  // ����Ϊ�˶���API��ֱ�Ӵ�����ʵ���ˡ�
	  _itpServer = new TpServer(TP_SERVER_PORT);

	  // Ʊ�ط�����Ӧ������disruptor event bus��
	  _itpServer.start();
     }

     private void stopRealServer() throws Exception {
	  if ( _itpServer != null ) {
	       _itpServer.stop();
	  }
     }

     // ���ط�Javaǿ���ں����������Լ������ӳ����쳣����֪��Java�ĳ����Ǻõģ�����
     // ...
     // ...
     // ...
     // Java�����ʦ�Ǿ�û��Ԥ�������кܶ���try ... catch (Exception e)��?
     private void connectToServer(final ChannelHandler sendRequest) throws Exception {
	  // ��������Ǵ�Netty���������ģ���ʱ����֪��ΪʲôҪ��ô����
	  ChannelFactory factory = new NioClientSocketChannelFactory(
	       Executors.newCachedThreadPool(),
	       Executors.newCachedThreadPool());
	  ClientBootstrap bootstrap = new ClientBootstrap(factory);
	  bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
		    public ChannelPipeline getPipeline() 
			 throws Exception {
			 // ��ѯG101
			 return Channels.pipeline(
			      // ʹ���Զ����bson��ʽ���л�
			      new ObjectBsonEncoder(),
			      new ObjectBsonDecoder(
				   ClassResolvers.cacheDisabled(
					getClass().getClassLoader())),
			      sendRequest);
		    }
	       });
	  // ���������ò����TCP�����ӣ��������ǵļƻ��ǽ�����³�UDP
	  // ���Ҳֱ�ӳ�Netty������ʾ��������ˣ�
	  bootstrap.setOption("tcpNoDelay", true);
	  bootstrap.setOption("keepAlive", true);
	  
	  // ���ӵ�������
	  bootstrap.connect(new InetSocketAddress(TP_SERVER_ADDRESS,
						  TP_SERVER_PORT));	      
     }
}
