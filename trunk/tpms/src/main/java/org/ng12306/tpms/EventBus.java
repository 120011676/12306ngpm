package org.ng12306.tpms;

import java.util.concurrent.*;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.*;
import java.io.*;
import org.ng12306.tpms.runtime.*;

public class EventBus {
	private static ObjectOutputStream _journal;
	private static RingBuffer<TicketQueryArgs> _ringBuffer;
	private static RingBuffer<TicketQueryResult> _outRingBuffer;
	private static Disruptor<TicketQueryArgs> _disruptor;
	private static Disruptor<TicketQueryResult> _disruptorRes;

	private static ITicketPoolManager _poolManger;

	// ��־�߳�
	static final EventHandler<TicketQueryArgs> _journalist = new EventHandler<TicketQueryArgs>() {
		public void onEvent(final TicketQueryArgs event, final long sequence,
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

	static final EventHandler<TicketQueryArgs> _eventProcessor = new EventHandler<TicketQueryArgs>() {
		public void onEvent(final TicketQueryArgs event, final long sequence,
				final boolean endOfBatch) throws Exception {
			// ���ݳ��κŲ�ѯ������ϸ��Ϣ

			ITicketPool pool = EventBus._poolManger.getPool(event);

			// �����ҵ���񣬶�����һ����Ӧ
			long s = _outRingBuffer.next();
			TicketQueryResult result = _outRingBuffer.get(s);

			if (pool != null) {
				TicketPoolQueryArgs poolArgs = pool
						.toTicketPoolQueryArgs(event);
				if (event.getAction() == TicketQueryAction.Query) {
					result.setHasTicket(pool.hasTickets(poolArgs));
				} else {
					TicketPoolTicket[] poolTickets = pool.book(poolArgs);
					result.setHasTicket(poolTickets.length > 0);
					result.setTickets(pool.toTicket(poolTickets));
				}
			} else {
				result.setHasTicket(false);
			}

			// ����Ӧ��Ϣ��������Ϣ������������Ϊ������Ҳ�п������첽�����
			result.setSequence(event.getSequence());
			_outRingBuffer.publish(s);
		}
	};

	// Ĭ�ϵ�������Ϣ����Ӧ��Ϣ���еĴ�С��2��13�η�
	private static int RING_SIZE = 2 << 13;
	private static final ExecutorService EXECUTOR = Executors
			.newCachedThreadPool();

	// ����Ϣ���з���һ����ѯ�����¼�
	
	public static void publishQueryEvent(TicketQueryArgs args) {
		long sequence = _ringBuffer.next();
		TicketQueryArgs event = _ringBuffer.get(sequence);
		args.copyTo(event);
		event.setSequence(sequence);
		
		_ringBuffer.publish(sequence);
	}

	public static void start() throws Exception {
		// ��disruptor����֮ǰ����־

		_poolManger = ServiceManager.getServices().getRequiredService(
				ITicketPoolManager.class);
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
		FileOutputStream fos = new FileOutputStream("eventbus.journal");
		_journal = new ObjectOutputStream(fos);
	}

	private static void startDisruptor() {
		// ���������ѯ��Ϣ��disruptor
		_disruptor = new Disruptor<TicketQueryArgs>(
				new EventFactory<TicketQueryArgs>(){

					@Override
					public TicketQueryArgs newInstance() {
						return new TicketQueryArgs();
					}}, EXECUTOR,
				new SingleThreadedClaimStrategy(RING_SIZE),
				new BlockingWaitStrategy());
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
		_disruptorRes = new Disruptor<TicketQueryResult>(
				new EventFactory<TicketQueryResult>(){

					@Override
					public TicketQueryResult newInstance() {
						return new TicketQueryResult();
					}}, EXECUTOR,
				new SingleThreadedClaimStrategy(RING_SIZE),
				new BlockingWaitStrategy());
		// �ڷ��ؽ����Ϣ��ʱ�򣬾Ͳ����κ���־�ͱ����ˡ�
		_outRingBuffer = _disruptorRes.start();
	}

}
