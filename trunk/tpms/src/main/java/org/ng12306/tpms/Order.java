package org.ng12306.tpms;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Order {
    // ���κ�
    public String train;

    // �ϳ�վ��
    public String departure;

    // �³�վ��
    public String termination;

	// ��λ��
	public String seat;

	// ���֤��
	public String id;

    // �ϳ�ʱ��
    public String date;
}