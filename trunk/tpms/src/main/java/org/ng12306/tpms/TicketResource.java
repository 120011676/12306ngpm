package org.ng12306.tpms;

import com.sun.jersey.spi.resource.Singleton;
import com.sun.jersey.api.json.JSONWithPadding;
import javax.ws.rs.*;
import javax.ws.rs.core.GenericEntity;

@Singleton
@Path("/ticket")
public class TicketResource {
    // ������Ҫ֧�ֿ�����ʣ�Ĭ�ϵ�json��ʽ��֧�ֿ�����ʣ���Ҫʹ��jsonp��ʽ��
	// ������Produces�����������"application/x-javascript; charset=UTF-8"��
	// ÿ��MIME���ͺ������charset��Ϊ�˷�ֹ����������������
	// 
	// ��������ϸ����һ�£���Ϊǰ̨��һ��single page web app�����е����ݶ���ͨ��
	// ajax����restful api�ķ�ʽ���á��������һ�����⣬ԭ��ϵͳ����û��װtomcat
	// ���õ�jersey restful�����Լ�������web��������Ӧajax���õģ���˾��޷�����
	// single page web app��restful service��ͬһ�������档
	// 
	// ��jsonp��ʽ���棬��ʵ������Ὣrestful service���ص�json������һ��javascript
	// ���봦������javascript��������Ϊ�﷨����ֹͣ�����������Ҫ��̬����һ��callback
	// �������ƹ�������ơ�
	// 
	// ����Ļ����ܲ�����⣬��������json��ʽ��restful service��������ʱ��
	// $.ajax({ url: 'restful-url', dataType: 'json' });
	// �˼ҷ��ص������¸�ʽ��
	// { key: 'value' }
	//
	// �������jsonp��ʽ����ʱ�������url������Ҫ��һ��������ָ��Ҫ��̬���ɵ�callback����
	// $.ajax({ url: 'restful-url?jsonpcallback=jpcb', dataType: 'jsonp', jsonp: 'jpcb' });
	// ���ص������¸�ʽ��
	// jpcb({key: 'value'});
	// 
    // �ο�����:http://weblogs.java.net/blog/felipegaucho/archive/2010/02/25/jersey-feat-jquery-jsonp
	//         http://api.jquery.com/jQuery.ajax/
	//         http://forum.jquery.com/topic/ajax-jsonpcallback
    @GET
    @Path("/id/{trainId}")
    @Produces({"application/x-javascript; charset=UTF-8", "application/json; charset=UTF-8"})
    public JSONWithPadding query(@QueryParam("jsonpcallback") @DefaultValue("jsonpcallback") String callback,
	                     @PathParam("trainId") String trainId) {
	Train[] trains = queryImpl(trainId);

	return new JSONWithPadding(
	    new GenericEntity<Train[]>(trains) {},
		callback);
    }

	@GET
    @Path("/make")
    @Produces({"application/x-javascript; charset=UTF-8", "application/json; charset=UTF-8"})
	public String make(@QueryParam("jsonpcallback") @DefaultValue("jsonpcallback") String callback, 
	                   @QueryParam("train") String train,
	                   @QueryParam("seat") String seat,
	                   @QueryParam("departure") String departure,
	                   @QueryParam("termination") String termination,
	                   @QueryParam("id") String id,
	                   @QueryParam("date") String date) {
		if ( train == null ) {
			throw new IllegalArgumentException("���κŲ�Ӧ��Ϊ�գ�");
		}
		if ( seat == null ) {
			throw new IllegalArgumentException("��λ�Ų�Ӧ��Ϊ�գ�");
		}
		if ( departure == null ) {
			throw new IllegalArgumentException("����վ��Ų�Ӧ��Ϊ�գ�");
		}
		if ( termination == null ) {
			throw new IllegalArgumentException("����վ�㲻Ӧ��Ϊ�գ�");
		}
		if ( id == null ) {
			throw new IllegalArgumentException("���֤�Ų�Ӧ��Ϊ�գ�");
		}
		if ( date == null ) {
			throw new IllegalArgumentException("�������ڲ�Ӧ��Ϊ�գ�");
		}

		return "true";
	}

	public Train[] queryImpl(String trainId) {
	Train[] trains = new Train[4];
	
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
	
	trains[0] = train;
	
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
	
	trains[1] = train;
	
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
	
	trains[2] = train;
	
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
	
	trains[3] = train;

	return trains;
	}
}
