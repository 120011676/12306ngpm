package org.ng12306.tpms;

import java.util.concurrent.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import static org.junit.Assert.*;
import org.junit.*;

public class DisruptorConceptProofTest
{
    private final static EventFactory<TestTicketEvent> TestFactory =
	new EventFactory<TestTicketEvent>() {
	public TestTicketEvent newInstance() {
	    return new TestTicketEvent();
	}
    };

    private long _journalistCount = 0L;
    // ������֤��־�߳̿��������һ���¼�ֵ
    private int _lastEventValue = 0;
    // ������֤��־�߳̿��������������̲߳������¼�
    private int _journalistValueSum = 0;
    // ���¼�������Ӳ��������Ա�߳�
    final EventHandler<TestTicketEvent> _journalist = 
	new EventHandler<TestTicketEvent>() {
	public void onEvent(final TestTicketEvent event, 
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	    _journalistCount++;
	    _lastEventValue = event.getValue();
	    _journalistValueSum += _lastEventValue;
	}
    };
    
    private long _replicatorCount = 0L;
    // ������֤�����߳̿��������������̲߳������¼�
    private int _replicatorValueSum = 0;
    // ���¼����͵����ݷ���������ı����߳�
    final EventHandler<TestTicketEvent> _replicator = 
	new EventHandler<TestTicketEvent>() {
	public void onEvent(final TestTicketEvent event,
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	    _replicatorCount++;
	    _replicatorValueSum += event.getValue();
	}
    };
    
    final EventHandler<TestTicketEvent> _eventProcessor = 
	new EventHandler<TestTicketEvent>() {
	public void onEvent(final TestTicketEvent event,
			    final long sequence,
			    final boolean endOfBatch) throws Exception {
	    System.out.println("[processor] " + new Long(sequence).toString());
	}
    };
    
    private int RING_SIZE = 128;
    private final ExecutorService EXECUTOR = 
	Executors.newCachedThreadPool();

    @Before
    public void setUp() throws Exception {
	_journalistCount = _replicatorCount = _lastEventValue = 0;
    }

    // �����������������"��ʾdisruptor�Ļ����÷�"������һ��
    @Test
    public void ��ʾdisruptor��dsl�÷�() throws Exception {
	Disruptor<TestTicketEvent> disruptor = 
	    new Disruptor<TestTicketEvent>
	    (
	     TestFactory,
	     EXECUTOR,
	     new SingleThreadedClaimStrategy(RING_SIZE),
	     new BlockingWaitStrategy()
	     );
	// ע����־�ͱ����߳�
	disruptor.handleEventsWith(_journalist);
	disruptor.handleEventsWith(_replicator);

	// ����disruptor,�ȴ�publish�¼�
	RingBuffer<TestTicketEvent> ringBuffer = disruptor.start();

	// ���һЩ�¼�
	for ( int i = 0; i < RING_SIZE; ++i ) {
	    long sequence = ringBuffer.next();
	    TestTicketEvent event = ringBuffer.get(sequence);
	    event.setValue(i);
	    ringBuffer.publish(sequence);
	}

	// ��ʽ�ȴ������߳�ִ�����,��Ϊ�����ڻ���֪����θ��õĵȴ�
	// ��������˵,Ӧ������ĳ��ʱ��ʹ��eventprocessor.halt������
	// ��Ϊ���ϵͳӦ���ǲ�ͣѭ�������
	Thread.sleep(1000);
	assertEquals(RING_SIZE, _journalistCount);
	assertEquals(RING_SIZE, _replicatorCount);

	// ������־�ͱ����߳�,Ӧ���Ǵ���ִ��ÿһ���¼���
	assertEquals(RING_SIZE - 1, _lastEventValue);

	// ����һ������,����ȷ�������¼��Ƿ�����Ѿ������ˣ�
	int expected = (0 + RING_SIZE - 1) * RING_SIZE / 2;
	assertEquals(expected, _journalistValueSum);
	assertEquals(expected, _replicatorValueSum);

	disruptor.halt();
    }
	
    @Test
    public void ��ʾdisruptor�Ļ����÷�() throws Exception {	
	RingBuffer<TestTicketEvent> ringBuffer = 
	    new RingBuffer<TestTicketEvent>
	    (
	     TestFactory,
	     new SingleThreadedClaimStrategy(RING_SIZE),
	     new BlockingWaitStrategy()
	    );

	SequenceBarrier barrier = ringBuffer.newBarrier();

	// ע����־�߳�
	BatchEventProcessor<TestTicketEvent> journalist = 
	    new BatchEventProcessor<TestTicketEvent>(ringBuffer,
						 barrier, 
						 _journalist);
	ringBuffer.setGatingSequences(journalist.getSequence());
	EXECUTOR.submit(journalist);

	// ע�ᱸ���߳�
	BatchEventProcessor<TestTicketEvent> replicator = 
	    new BatchEventProcessor<TestTicketEvent>(ringBuffer,
						 barrier,
						 _replicator);
	ringBuffer.setGatingSequences(replicator.getSequence());
	EXECUTOR.submit(replicator);

	for ( int i = 0; i < RING_SIZE; ++i ) {
	    long sequence = ringBuffer.next();
	    TestTicketEvent event = ringBuffer.get(sequence);
	    event.setValue(i);
	    ringBuffer.publish(sequence);
	}

	// ��ʽ�ȴ������߳�ִ�����,��Ϊ�����ڻ���֪����θ��õĵȴ�
	// ��������˵,Ӧ������ĳ��ʱ��ʹ��eventprocessor.halt������
	// ��Ϊ���ϵͳӦ���ǲ�ͣѭ�������
	Thread.sleep(1000);
	assertEquals(RING_SIZE, _journalistCount);
	assertEquals(RING_SIZE, _replicatorCount);

	// ������־�ͱ����߳�,Ӧ���Ǵ���ִ��ÿһ���¼���
	assertEquals(RING_SIZE - 1, _lastEventValue);

	int expected = (0 + RING_SIZE - 1) * RING_SIZE / 2;
	assertEquals(expected, _journalistValueSum);
	assertEquals(expected, _replicatorValueSum);
    }
}
