package org.ng12306.tpms;

// ������ȫ��ת��joda-time���java�Դ�����������̫�����ˣ�
import org.joda.time.DateTime;

// ���ݳ��β�ѯ��Ʊ���¼�
public class TicketQueryEvent extends TicketEvent
{
    // ���κ�
    public String trainId;
    // Ҫ��ѯ����ʼ����
    public DateTime startDate;
    // Ҫ��ѯ����ֹ����
    public DateTime endDate;

    public TicketQueryEvent() {
	super(TicketEventType.QueryByTrain);
    }
}
