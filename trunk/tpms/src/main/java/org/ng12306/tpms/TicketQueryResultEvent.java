package org.ng12306.tpms;

public class TicketQueryResultEvent extends TicketEvent
{
    // ��ѯ���ĳ�����Ϣ
    public Train[] trains;
    
    public TicketQueryResultEvent() {
	super(TicketEventType.QueryResult);
    }
}
