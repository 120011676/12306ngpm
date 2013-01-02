package org.ng12306.tpms;

import org.joda.time.DateTime;

//
// �����Ʊ���ݿ⣬Ӧ��֧����ɾ�Ĳ�Ȳ�����
// ��Ʊ���ݸ�����ƽʱ��Ϊ��mysql���ݿⲻһ���������Դ�mysql������ݴ���
// Ҳ����ֱ�Ӵ��ڴ����ݿ��ﴴ����
//
// ������Ὣ�乫��������ȡ��һ���ӿ�IRepository����ʹ��Ioc�ķ�ʽע��
// ������ֻ�����¶��ϵķ�װ��ʽ�����ż�������Ƶĺ����ơ�
// 
public class TicketRepository {
    static {
	prepareFakeTicketPool();	
    }
    
    /**
     * ���ݳ��κźͳ˳����ڲ�ѯ���ε���ϸ��Ϣ
     * @param name Ҫ��ѯ�ĳ��κ�
     * @param startDate Ҫ��ѯ�ĳ˳���ʼ����
     * @param endDate Ҫ��ѯ�Ľ�������
     * @return һ������������ϸ��Ϣ��Train���󣬷��򷵻�null
     * @see Train
     */
    public static Train queryTrain(String name, 
				   DateTime startDate, 
				   DateTime endDate) {
	for ( int i = 0; i < _trains.length; ++i ) {
	    Train train = _trains[i];		
	    if ( train.name.compareTo(name) == 0 ) {
		return train;
	    }
	}

	return null;
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
}
