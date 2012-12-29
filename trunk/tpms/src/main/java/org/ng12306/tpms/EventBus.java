package org.ng12306.tpms;

import java.util.concurrent.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import org.joda.time.DateTime;

public class EventBus {
    private static ObjectOutputStream _journal;
    private static RingBuffer<TicketQueryEvent> _ringBuffer;
    private static RingBuffer<TicketQueryResultEvent> _outRingBuffer;
    private static Disruptor<TicketQueryEvent> _disruptor;
    private static Disruptor<TicketQueryResultEvent> _disruptorRes;
    
    // ��־�߳�
    static final EventHandler<TicketQueryEvent> _journalist = 
	new EventHandler<TicketQueryEvent>() {
	public void onEvent(final TicketQueryEvent event, 
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
    static final EventHandler<TicketQueryEvent> _replicator = 
	new EventHandler<TicketQueryEvent>() {
	public void onEvent(final TicketQueryEvent event,
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	    // TODO: ������ʵ�ֱ����̵߳��߼�
	}
    };

    static final EventHandler<TicketQueryEvent> _eventProcessor = 
	new EventHandler<TicketQueryEvent>() {
	public void onEvent(final TicketQueryEvent event,
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	    // ���ݳ��κŲ�ѯ������ϸ��Ϣ
	    Train train = TicketRepository.queryTrain(event.trainId,
						      event.startDate,
						      event.endDate);

	    // �����ҵ���񣬶�����һ����Ӧ
	    long s = _outRingBuffer.next();
	    TicketQueryResultEvent e = _outRingBuffer.get(s);	    
	    // ����Ӧ��Ϣ��������Ϣ������������Ϊ������Ҳ�п������첽�����
	    e.sequence = event.sequence;

	    // �ҵ��˵Ļ���������Ӧ��Ϣ���������һ��������ϸ��Ϣ����
	    if ( train != null ) {	       
		e.trains = new Train[1];
		e.trains[0] = train;
	    } else {
		// ��������Ϊnull������ǰ̨jersey��restful����
		// �����л������ʱ����ܻ���������Ը����һ�������顣
		e.trains = new Train[0];
	    }

	    _outRingBuffer.publish(s);
	}
    };
    
    // Ĭ�ϵ�������Ϣ����Ӧ��Ϣ���еĴ�С��2��13�η�
    private static int RING_SIZE = 2 << 13;
    private static final ExecutorService EXECUTOR = 
	Executors.newCachedThreadPool();

    // ����Ϣ���з���һ����ѯ�����¼�
    // TODO: ��publicXXXEvent�ĳ��첽�ģ�Ӧ�÷���void���ͣ��첽���ز�ѯ�����
    public static Train[] publishQueryEvent(String trainId,
					    DateTime startDate,
					    DateTime endDate) {
	long sequence = _ringBuffer.next();
	TicketQueryEvent event = _ringBuffer.get(sequence);
	event.sequence = sequence;
	event.trainId = trainId;
	event.startDate = startDate;
	event.endDate = endDate;
	_ringBuffer.publish(sequence);

	// ����Ӧ�õ���Ϊֹ�������һ���֪������޸�jersey��ʹ��
	// �����첽����Դrestful��������߷��ؽ�������ֻ��
	// ������ͬ���ķ�ʽ
	return waitForResponse(sequence).trains;
    }

    public static void start() throws Exception {
	// ��disruptor����֮ǰ����־
	openJournal();
	startDisruptor();
    }
    
    public static void shutdown() throws Exception {
	// �ȹرյ�disruptor���ٹر���־�ļ�
	// ���������־�̺߳�disruptor�ر�ͬʱ���е����
	_disruptor.shutdown();
	_disruptorRes.shutdown();
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
	    new Disruptor<TicketQueryEvent>
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

	// �������ز�ѯ�����Ϣ��disruptor
	_disruptorRes = 
	    new Disruptor<TicketQueryResultEvent>
	    (
	     TicketPoolService.QueryResultFactory,
	     EXECUTOR,
	     new SingleThreadedClaimStrategy(RING_SIZE),
	     new BlockingWaitStrategy()
	    );
	// �ڷ��ؽ����Ϣ��ʱ�򣬾Ͳ����κ���־�ͱ����ˡ�
	_outRingBuffer = _disruptorRes.start();
    }

    // ��֪���������jersey�첽���ú�ɾ���������
    private static TicketQueryResultEvent waitForResponse(long sequence) {
	while ( true ) {
	    long s = _outRingBuffer.getCursor();
	    TicketQueryResultEvent event = _outRingBuffer.get(s);
	    if ( event.sequence == sequence ) {
		return event;
	    }
	}
    }

}
