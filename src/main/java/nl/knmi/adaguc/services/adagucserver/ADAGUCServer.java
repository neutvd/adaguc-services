package nl.knmi.adaguc.services.adagucserver;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.knmi.adaguc.config.MainServicesConfigurator;
import nl.knmi.adaguc.security.AuthenticatorFactory;
import nl.knmi.adaguc.security.AuthenticatorInterface;
import nl.knmi.adaguc.security.user.UserManager;
import nl.knmi.adaguc.tools.CGIRunner;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;



/**
 * 
 * @author maartenplieger
 *
 */
public class ADAGUCServer extends HttpServlet{

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * Runs the ADAGUC WMS server as executable on the system. 
   * Emulates the behavior of scripts in a traditional cgi-bin directory of apache http server.
   * @param request, the HttpServletRequest to obtain querystring parameters from.
   * @param response Can be null, when given the content-type for the response will be set. 
   * Results are not sent to this stream, this is done by outputStream parameter
   * @param queryString The querystring for the CGI script
   * @param outputStream A standard byte output stream in which the data of stdout is captured. 
   * When null, it will be set to response.getOutputStream().
   * @throws Exception
   */
  public static void runADAGUCWMS(HttpServletRequest request,HttpServletResponse response,String queryString,OutputStream outputStream) throws Exception{
	  runADAGUC(request,response, queryString, outputStream, ADAGUCServiceType.WMS);
  }
  /**
   * Runs the ADAGUC WCS server as executable on the system. 
   * Emulates the behavior of scripts in a traditional cgi-bin directory of apache http server.
   * @param request, the HttpServletRequest to obtain querystring parameters from.
   * @param response Can be null, when given the content-type for the response will be set. 
   * Results are not sent to this stream, this is done by outputStream parameter
   * @param queryString The querystring for the CGI script
   * @param outputStream A standard byte output stream in which the data of stdout is captured. 
   * When null, it will be set to response.getOutputStream().
   * @throws Exception
   */
  public static void runADAGUCWCS(HttpServletRequest request,HttpServletResponse response,String queryString,OutputStream outputStream) throws Exception{
	  runADAGUC(request,response, queryString, outputStream, ADAGUCServiceType.WCS);
  }
  enum ADAGUCServiceType{
	  WMS, WCS
  }
  public static void runADAGUC(HttpServletRequest request,HttpServletResponse response,String queryString,OutputStream outputStream, ADAGUCServiceType serviceType) throws Exception{
  
    Debug.println("runADAGUCWMS");
    List<String> environmentVariables = new ArrayList<String>();
    String userHomeDir="/tmp/";
    
//    AuthenticatorInterface authenticator = AuthenticatorFactory.getAuthenticator(request);
//	if(authenticator != null){
//		userHomeDir = UserManager.getUser(authenticator).getHomeDir();
//	}
	
    String homeURL=MainServicesConfigurator.getServerExternalURL();
    String adagucExecutableLocation = ADAGUCConfigurator.getADAGUCExecutable();
    Debug.println("adagucExecutableLocation: "+adagucExecutableLocation);
    
    if(adagucExecutableLocation == null){
    	Debug.errprintln("Adagucserver executable not configured");
    	throw new Exception("Adagucserver executable not configured");
    }
    
    File f=new File(adagucExecutableLocation);
    if(f.exists() == false || f.isFile() == false){
    	Debug.errprintln("Adagucserver executable not found");
    	throw new Exception("Adagucserver executable not found");
    }
    
    
    
    if(response == null && outputStream == null){
    	throw new Exception("Either response or outputstream needs to be set");
    }
    
    if(request == null && queryString == null){
    	throw new Exception("Either request or queryString needs to be set");
    }
    
    if(outputStream == null){
    	outputStream = response.getOutputStream();
    }
    
    if(queryString == null){
    	queryString = request.getQueryString();
    }
    
    
 
    environmentVariables.add("HOME="+userHomeDir);
    environmentVariables.add("QUERY_STRING="+queryString);
    if(serviceType == ADAGUCServiceType.WMS){
    	environmentVariables.add("ADAGUC_ONLINERESOURCE="+homeURL+"/wms?");
    }
    if(serviceType == ADAGUCServiceType.WCS){
    	environmentVariables.add("ADAGUC_ONLINERESOURCE="+homeURL+"/wcs?");
    }
    
    Tools.mksubdirs(userHomeDir+"/adaguctmp/");
    environmentVariables.add("ADAGUC_TMP="+userHomeDir+"/adaguctmp/");
    
    String[] configEnv = ADAGUCConfigurator.getADAGUCEnvironment();
    if(configEnv == null){
    	Debug.println("ADAGUC environment is not configured");
    }else{
    	for(int j=0;j<configEnv.length;j++)environmentVariables.add(configEnv[j]);    
    }
    String commands[] = {adagucExecutableLocation};
  
    String[] environmentVariablesAsArray = new String[ environmentVariables.size() ];
    environmentVariables.toArray( environmentVariablesAsArray );
    CGIRunner.runCGIProgram(commands,environmentVariablesAsArray,userHomeDir,response,outputStream,null);
  }
  
  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request,response);
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    Debug.println("Handle ADAGUC WMS requests");
    OutputStream out1 = null;
    //response.setContentType("application/json");
    try {
      out1 = response.getOutputStream();
    } catch (IOException e) {
      Debug.errprint(e.getMessage());
      return;
    }
  
    try {
      ADAGUCServer.runADAGUCWMS(request,response,request.getQueryString(),out1);

    } catch (Exception e) {
      response.setStatus(401);
      try {
        out1.write(e.getMessage().getBytes());
      } catch (Exception e1) {
        Debug.errprintln("Unable to write to stream");
        Debug.printStackTrace(e);
      }
    }    
  }


}
