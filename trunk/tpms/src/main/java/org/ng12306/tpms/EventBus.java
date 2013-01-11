package org.ng12306.tpms;

import java.util.concurrent.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import org.joda.time.DateTime;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import org.ng12306.tpms.runtime.ServiceManager;
import org.ng12306.tpms.runtime.TicketQueryArgs;
import org.ng12306.tpms.runtime.TicketQueryAction;
import org.ng12306.tpms.runtime.TicketPoolQueryArgs;
import org.ng12306.tpms.runtime.ITicketPoolManager;
import org.ng12306.tpms.runtime.ITicketPool;

public class EventBus {
     private static ObjectOutputStream _journal;
     private static RingBuffer<TicketQueryArgs> _ringBuffer;
     private static Disruptor<TicketQueryArgs> _disruptor;
     private static ITicketPoolManager _poolManger;
     
     // ��־�߳�
     static final EventHandler<TicketQueryArgs> _journalist = 
	  new EventHandler<TicketQueryArgs>() {
	  public void onEvent(final TicketQueryArgs event, 
			      final long sequence,
			      final boolean endOfBatch) throws Exception {
	       // TODO: ��Ҫȷ�����������ʱ�����е����ݶ���Ӳ����
	       // ��Ϊ��Ӳ������һ������������Ҫȷ����ʹ�������Ҳ�ܰѻ����������
	       // д��Ӳ���ϣ�����ò���ִ�����ϵͳ�ܹ������ڳ������ʱflush���棬���
	       // ��Ҫ������֤��
	       _journal.writeObject(event);
	  }
     };
     
     // ���¼����͵����ݷ���������ı����߳�
     static final EventHandler<TicketQueryArgs> _replicator = new EventHandler<TicketQueryArgs>() {
	  public void onEvent(final TicketQueryArgs event, final long sequence,
			      final boolean endOfBatch) throws Exception {
	       // TODO: ������ʵ�ֱ����̵߳��߼�
	  }
     };

    static final EventHandler<TicketQueryArgs> _eventProcessor = 
	new EventHandler<TicketQueryArgs>() {
	public void onEvent(final TicketQueryArgs event,
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	     // ���ݳ��κŲ�ѯ������ϸ��Ϣ	       
	     ITicketPool pool = EventBus._poolManger.getPool(event);
	     
	     // TODO: ��δ����������飬��Ϊ��ѯ��ƱӦ�÷�����Ʊ�ĳ����б�
	     if (pool != null) {
		  TicketPoolQueryArgs poolArgs = pool
		       .toTicketPoolQueryArgs(event);
		  if (event.getAction() == TicketQueryAction.Query) {
		       boolean result = pool.hasTickets(poolArgs);
		       // result ֱ�Ӷ���result�ˡ�
		  }
	     }

	     /*
	     // �����ҵ���񣬶�����һ����Ӧ
	    Train[] trains = null;
	    if ( train != null ) {
		trains = new Train[] { train };
	    } else { 
		trains = new Train[0];
	    }

	    // ����ѯ���ֱ��д�������¼��ϵĿͻ��˶˿��ϡ�
	    // ������Ľ�������ݣ���Ϊû�б�Ҫ������ͻ����ղ���������
	    // ����Ҫ�ٷ�һ�Σ�����Ļ����ѵ�Ʊ���ڻָ�ʱ��Ҫ��������
	    // ���ط���Ӧ��
	    ChannelFuture future = event.channel.write(trains);	    
	    future.addListener(ChannelFutureListener.CLOSE);
	     */
	}
    };
    
    // Ĭ�ϵ�������Ϣ����Ӧ��Ϣ���еĴ�С��2��13�η�
    private static int RING_SIZE = 2 << 13;
    private static final ExecutorService EXECUTOR = 
	Executors.newCachedThreadPool();

    // ����Ϣ���з���һ����ѯ�����¼�
    // TODO: ��publicXXXEvent�ĳ��첽�ģ�Ӧ�÷���void���ͣ��첽���ز�ѯ�����
    public static void publishQueryEvent(TicketQueryArgs args) {
	long sequence = _ringBuffer.next();
	TicketQueryArgs event = _ringBuffer.get(sequence);
	args.copyTo(event);
	// event.sequence = sequence;
	event.setSequence(sequence);

	// ����Ϣ�ŵ����ֶ�����Ա㴦��
	_ringBuffer.publish(sequence);
    }

    public static void start() throws Exception {
	 _poolManger = ServiceManager.getServices().getRequiredService(
	      ITicketPoolManager.class);

	 // ��disruptor����֮ǰ����־
	 openJournal();
	 startDisruptor();
    }
    
    public static void shutdown() throws Exception {
	// �ȹرյ�disruptor���ٹر���־�ļ�
	// ���������־�̺߳�disruptor�ر�ͬʱ���е����
	_disruptor.shutdown();
	// _disruptorRes.shutdown();
	_journal.close();
    }

    private static void openJournal() throws Exception {
	// Ӧ����ֻ����
	FileOutputStream fos = 
	    new FileOutputStream("eventbus.journal");
	_journal = new ObjectOutputStream(fos);
    }
    
    private static void startDisruptor() {       
	// ���������ѯ��Ϣ��disruptor
	_disruptor = 
	    new Disruptor<TicketQueryArgs>
	    (
	     TicketPoolService.QueryFactory,
	     EXECUTOR,
	     new SingleThreadedClaimStrategy(RING_SIZE),
	     new BlockingWaitStrategy()
	     );
	// ע����־�ͱ����߳�
	_disruptor.handleEventsWith(_journalist);
	_disruptor.handleEventsWith(_replicator);

	// �¼������߳�ֻ������־�ͱ����߳�֮������
	_ringBuffer = _disruptor.getRingBuffer();
	SequenceBarrier barrier = _ringBuffer.newBarrier();
	_disruptor.handleEventsWith(_eventProcessor);

	// ����disruptor,�ȴ�publish�¼�
	_disruptor.start();
    }
}
