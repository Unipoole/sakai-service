

import java.rmi.RemoteException;

import javax.xml.rpc.ParameterMode;
import javax.xml.rpc.ServiceException;
import javax.xml.rpc.encoding.XMLType;

import junit.framework.Assert;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 *
 * @author Jaco
 */
public class UnipooleScriptTest {
	
    public UnipooleScriptTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    @Ignore
    public void createSakaiSession() {

        String sessionId = getSessionId();
//        System.out.println(sessionId);
        
    }
    
    private String getSessionId(){
    	Service service = new Service();
        String id = "oc_admin";
        String pw = "SpringCl@w";
        Call nc = null;
        String sessionId = "";
        try {
            nc = (Call) service.createCall();
            nc.setTargetEndpointAddress("http://unipoole.opencollab.co.za:8080/sakai-axis/SakaiLogin.jws?wsdl");
            nc.removeAllParameters();
            nc.setOperationName("login");
            nc.addParameter("id", XMLType.XSD_STRING, ParameterMode.IN);
            nc.addParameter("pw", XMLType.XSD_STRING, ParameterMode.IN);
            nc.setReturnType(XMLType.XSD_STRING);
            sessionId = (String) nc.invoke(new Object[] { id, pw });            
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} 
        return sessionId;
    }
    
    @Test
    @Ignore
    public void getAllSitesForSearchCriteria() {    	

        String sessionId = getSessionId();
        String searchCriteria = "AAC";
        String sites = getAllSites(sessionId, searchCriteria);

//        System.out.println(sites);
    }    

	private String getAllSites(String sessionId, String searchCriteria) {

		// AAC411A-06-Y2
		// 1. The first is the module code (AAC411A), 7 characters
		//
		// 2. The second “06” is the year, here 2006
		//
		// 3. The third is the semester “Y2”. We have four periods: (the 0 for
		// Y1 is what is stored in the student database, don’t think you are
		// going to need that but just in case).
		//
		// Y1 = 0
		// Year course
		// Y2 = 6
		// Year course starting in the second part of the year.
		// S1 = 1
		// Semester 1
		// S2 = 2
		// Semester 2
		// Append: moduleCode(AAC411A), year(06), semester (Y1)

		Service service = new Service();
		Call nc = null;
		String sites = "";
		try {
			nc = (Call) service.createCall();

			nc.setTargetEndpointAddress("http://unipoole.opencollab.co.za:8080/sakai-axis/UnipooleScript.jws?wsdl");
			nc.removeAllParameters();
			nc.setOperationName("getAllSitesForSearchCriteria");
			nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
			nc.addParameter("searchCriteria", XMLType.XSD_STRING,
					ParameterMode.IN);
			nc.setReturnType(XMLType.XSD_STRING);
			sites = (String) nc.invoke(new Object[] { sessionId,
					searchCriteria });
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return sites;
	}

    @Test
    @Ignore
    public void getAllPagesAndTools() {

        String sessionId = getSessionId();
        String searchCriteria = "AAC411A-06-Y2";
        String sites = getAllSites(sessionId, searchCriteria);        

        Service service = new Service();
        Call nc = null;
        String pages = "";     
        try {
			nc = (Call) service.createCall();
	        nc.setTargetEndpointAddress("http://unipoole.opencollab.co.za:8080/sakai-axis/UnipooleScript.jws?wsdl");
	        nc.removeAllParameters();
	        nc.setOperationName("getPagesAndToolsForSite");
	        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
	        nc.addParameter("siteid", XMLType.XSD_STRING, ParameterMode.IN);
	        nc.setReturnType(XMLType.XSD_STRING);
	        pages = (String) nc.invoke(new Object[] { sessionId, searchCriteria });     
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}        
//        System.out.println(pages);
    }
    
    @Test
    @Ignore
    public void postEvents() {

        String sessionId = getSessionId();
        String event = "UnipooleScript";
        String resource = "test";
    	boolean modify = false;
    			
        Service service = new Service();
        Call nc = null;
        String message = "";  
        try {
			nc = (Call) service.createCall();
	        nc.setTargetEndpointAddress("http://unipoole.opencollab.co.za:8080/sakai-axis/UnipooleScript.jws?wsdl");
	        nc.removeAllParameters();
	        nc.setOperationName("postEvent");
	        nc.addParameter("sessionid", XMLType.XSD_STRING, ParameterMode.IN);
	        nc.addParameter("event", XMLType.XSD_STRING, ParameterMode.IN);
	        nc.addParameter("resource", XMLType.XSD_STRING, ParameterMode.IN);
	        nc.addParameter("modify", XMLType.XSD_BOOLEAN, ParameterMode.IN);
	        nc.setReturnType(XMLType.XSD_STRING);
	        message = (String) nc.invoke(new Object[] { sessionId, event, resource, modify });  
		} catch (ServiceException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}      

//        System.out.println(message);
//      assert...;
    }
}
