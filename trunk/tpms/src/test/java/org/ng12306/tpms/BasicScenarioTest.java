package org.ng12306.tpms;

import java.util.concurrent.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import static org.junit.Assert.*;
import org.junit.*;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import org.joda.time.DateTime;

public class BasicScenarioTest 
{
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
	    String id = event.trainId;
	    for ( int i = 0; i < _trains.length; ++i ) {
		Train train = _trains[i];		
		if ( train.name.compareTo(id) == 0 ) {
		    long s = _outRingBuffer.next();
		    TicketQueryResultEvent e = _outRingBuffer.get(s);
		    e.trains = new Train[1];
		    e.trains[0] = train;
		    e.sequence = event.sequence;
		    _outRingBuffer.publish(s);
		    break;
		}
	    }
	}
    };
    
    private static int RING_SIZE = 128;
    private static final ExecutorService EXECUTOR = 
	Executors.newCachedThreadPool();
    
    @BeforeClass
    public static void setUp() throws Exception {
	// ���ݿ��ǿ϶���Ҫ�����е�����֮ǰ��׼������
	prepareFakeTicketPool();
	// ��disruptor����֮ǰ����־
	openJournal();
	startDisruptor();
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
	_disruptor.shutdown();
	_disruptorRes.shutdown();
	_journal.close();
    }

    @Test
    public void ʵ��һ�����β�ѯ��Disruptor����() throws Exception {
	// 1.��event bus����һ����ѯ�¼�
	long sequence = _ringBuffer.next();
	TicketQueryEvent event = _ringBuffer.get(sequence);
	event.sequence = sequence;
	event.trainId = "G101";	    
	event.startDate = new DateTime(2012, 12, 8, 16, 0, 0);
	event.endDate = new DateTime(2012, 12, 9, 0, 0, 0);
	_ringBuffer.publish(sequence);

	// 2. Ȼ��ȴ�һ��ʱ�䣬�Ա������߳̿��Դ�����
	Thread.sleep(1);
	
	// 3. ��֤�����
	TicketQueryResultEvent response = waitForResponse(sequence);
	Train train = response.trains[0];
	assertEquals("G101", train.name);
	assertEquals("������", train.departure);
	assertEquals("�Ϻ�����", train.termination);
	assertEquals("07:00", train.departureTime); 
	assertEquals("12:23", train.arrivalTime);
	assertEquals(2, train.availables.length);
    }

    private TicketQueryResultEvent waitForResponse(long sequence) {
	while ( true ) {
	    long s = _outRingBuffer.getCursor();
	    TicketQueryResultEvent event = _outRingBuffer.get(s);
	    if ( event.sequence == sequence ) {
		return event;
	    }
	}
    }
    
    private static void openJournal() throws Exception {
	// Ӧ����ֻ����
	FileOutputStream fos = 
	    new FileOutputStream("basicscenariotest.journal");
	_journal = new ObjectOutputStream(fos);
    }

    private static Train[] _trains;
    private static void prepareFakeTicketPool() {
	_trains = new Train[4];
	
	Train train = new Train();
	train.name = "G101";
	train.departure = "������";
	train.departureTime = "07:00";
	train.termination = "�Ϻ�����";
	train.arrivalTime = "12:23";
	
	String[][] availables = new String[2][2];
	availables[0][0] = "��������";
	availables[0][1] = "��Ʊ";
	availables[1][0] = "һ������";
	availables[1][1] = "3";
	train.availables = availables;
	
	_trains[0] = train;
	
	train = new Train();
	train.name = "G105";
	train.departure = "������";
	train.departureTime = "07:30";
	train.termination = "�Ϻ�����";
	train.arrivalTime = "13:07";
	
	availables = new String[2][2];
	availables[0][0] = "��������";
	availables[0][1] = "��Ʊ";
	availables[1][0] = "һ������";
	availables[1][1] = "5";
	train.availables = availables;
	
	_trains[1] = train;
	
	train = new Train();
	train.name = "D365";
	train.departure = "������";
	train.departureTime = "07:35";
	train.termination = "�Ϻ�����";
	train.arrivalTime = "15:42";
	
	availables = new String[4][2];
	availables[0][0] = "��������";
	availables[0][1] = "��Ʊ";
	availables[1][0] = "һ������";
	availables[1][1] = "��Ʊ";
	availables[2][0] = "������";
	availables[2][1] = "��Ʊ";
	availables[3][0] = "������";
	availables[3][1] = "��Ʊ";
	train.availables = availables;
	
	_trains[2] = train;
	
	train = new Train();
	train.name = "T109";
	train.departure = "����";
	train.departureTime = "19:33";
	train.termination = "�Ϻ�";
	train.arrivalTime = "10:26";
	    
	availables = new String[8][2];
	availables[0][0] = "Ӳ��";
	availables[0][1] = "��Ʊ";
	availables[1][0] = "Ӳ����";
	availables[1][1] = "��Ʊ";
	availables[2][0] = "Ӳ����";
	availables[2][1] = "��Ʊ";
	availables[3][0] = "Ӳ����";
	availables[3][1] = "��Ʊ";
	availables[4][0] = "������";
	availables[4][1] = "��Ʊ";
	availables[5][0] = "������";
	availables[5][1] = "��Ʊ";
	availables[6][0] = "�߼�������";
	availables[6][1] = "��Ʊ";
	availables[7][0] = "�߼�������";
	availables[7][1] = "5";
	train.availables = availables;
	
	_trains[3] = train;       
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
}
