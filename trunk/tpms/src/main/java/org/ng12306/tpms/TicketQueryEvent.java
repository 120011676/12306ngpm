package org.ng12306.tpms;

import java.util.Date;

public class TicketQueryEvent extends TicketEvent
{
    // ���κ�
    public String trainId;
    // Ҫ��ѯ����ʼ����
    public Date startDate;
    // Ҫ��ѯ����ֹ����
    public Date endDate;

    public TicketQueryEvent() {
	super(TicketEventType.QueryByTrain);
    }
}
