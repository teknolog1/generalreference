package carShare.web;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import carShare.web.dao.DALDriver;
import carShare.web.domain.ErrorDetailedInfo;
import carShare.web.domain.User_Record;
import carShare.web.domain.MailerThreadObject;
import carShare.web.domain.CarShareGenericException;
import carShare.web.domain.CalendarUtilities;
import carShare.web.domain.User;




/**
 * User Registration and login. Spring framework.. a controller with authentication
 */
@Controller 
@SessionAttributes("user")
public class RegisterUser  {
	// auth. for login, db and logs.
	@Autowired
	private DALDriver dalDriver;
	@Autowired
	private AuthenticationManager authenticationManager;
	private Logger genericLog = org.slf4j.LoggerFactory.getLogger("genericLog");
	
	/**
	 * Fetch and show registration form.
	 */
    @RequestMapping(value="/NewUser", method=RequestMethod.GET)
    public ModelAndView bindRegistrationForm(Model toUpdate) { 
  
    	genericLog.info("GET - Registration");
    	
    	// needs a user to save on post
    	toUpdate.addAttribute("user", new User());
    	ModelAndView mv = new ModelAndView("/NewUser", "model" ,toUpdate.asMap());

    	return mv;
    }
    
    /* 
     * POST-- a more simple version..
     */
    @RequestMapping(value="/NewUser2", method=RequestMethod.POST)
    public ModelAndView submitRegistrationForm2(Model model,HttpServletRequest request, HttpServletResponse response)  {
    
    	
    	genericLog.info("POST - SUBMIT register for simple user..");
    	User usr = new User();
    	String uId = request.getParameter("unameSimpl");
    	usr.setUsrid(uId);
    	String pId = request.getParameter("passSimpl");
    	usr.setPassword(pId);
    	String eId = request.getParameter("emailSimpl");
    	usr.setEmail(eId);
    	usr.setPhonenum("000-000-0000");
    	usr.setFirstname("Anonymous");
    	usr.setLastname("Anonymous");
    	usr.setCountry("United States");
    	usr.setState("CA");
    	usr.setAddr("Please enter home address.");
    	usr.setCity("NotEntered");
    	usr.setZipcode("93000");
    	
    	
    	
    	try
    	{
    		// if the add was success or no
    		int rezult= dalDriver.add(usr);
    		if (rezult == -1) // sanitize error for preexisting user..
    		{
        		ErrorDetailedInfo paramExceptionDesc = new ErrorDetailedInfo();
        		paramExceptionDesc.setHasException(true);
        		paramExceptionDesc.setExceptionString("This user already exists in database");
        		model.addAttribute("ErrOut", paramExceptionDesc);
        		return new ModelAndView("/ErrorPage", "model", model);
    		}
    	}
    	catch (CarShareGenericException sqlSideException)
    	{
			ArrayList<String> listOfParamErrors = new ArrayList<String>();
			listOfParamErrors.add(sqlSideException.getMessage());
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setErrors(listOfParamErrors );
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	catch (Exception sqlException)
    	{
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setExceptionString(sqlException.toString());
    		errorDescModel.setHasException(true);
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	
    	
    	// after user add, log the user in..
    	try {
    		Authentication authResult = new UsernamePasswordAuthenticationToken(
    				usr.getUsrid(), usr.getPassword());
    		Authentication success = authenticationManager.authenticate(authResult);
    		SecurityContextHolder.getContext().setAuthentication(success );
    	} catch (AuthenticationException authExcept) {
    		ArrayList<String> listOfParamErrors = new ArrayList<String>();
    		listOfParamErrors.add("Failed Login...");
    		
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setErrors(listOfParamErrors);
    		errorDescModel.setExceptionString(authExcept.toString());
    		errorDescModel.setHasException(true);
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	
    	CalendarUtilities.ProcessTimezoneForLogin(request, genericLog, usr, "2");
    	
    	Map<String, Object> pageData = new HashMap<String, Object>();
        
    	pageData.put("user", usr);
        
        // feedback workstuffs.
    	List<User_Record> feedbackHistory = this.dalDriver.getFeedBackForUser(request.getUserPrincipal().getName());
    	
    	int positiveRate = 0, negativeRate = 0, neutralRate = 0;
		
		// okay count history return to add additional attributes.
		for (int i=0; i < feedbackHistory.size(); i++)
		{
			// neutral positive and negative
			if (feedbackHistory.get(i).getFeedback()  > 0) positiveRate++; 
			else if (feedbackHistory.get(i).getFeedback() == 0)
				neutralRate++;
			else negativeRate++;
		}
		
		pageData.put("positiveRate", positiveRate);
		pageData.put("negativeRate", negativeRate);
		pageData.put("neutralRate", neutralRate);
	    pageData.put("feedbackHistory", feedbackHistory);

        
        MailerThreadObject threadMailer = new MailerThreadObject(usr.getEmail(), 
        		"Welcome to Carshare\r\n", 
        		"Welcome " + usr.getUsrid() + "\r\n " +
        				"Create or share a trip with carshare now.\r\n" +
        				"  Sincerely,\r\n The CarshareTeam\r\n",
        		"<html><body>Welcome <span>" + usr.getUsrid() + "</span> <p> Create or share a trip with carshare now.</p>  Sincerely,<br> The CarshareTeam </body></html>", genericLog);
        
        threadMailer.start();
        
        // display user profile for logged in user
        return new ModelAndView("myprofile", "model", pageData);

    }
    /**
     * Post the registration for a particular user.
     */
    @RequestMapping(value="/NewUser", method=RequestMethod.POST)
    public ModelAndView submitRegistrationForm(@ModelAttribute("user" ) User usr, BindingResult result, Model model,HttpServletRequest request, HttpServletResponse response)  {
    
    	
    	genericLog.info("POST - SUBMIT register for:" + usr.getUsrid());
    	if (result.hasErrors())
    	{ 
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.populateErrorsFromBinding(result);
    		model.addAttribute("ErrOut", errorDescModel);
    		
			return new ModelAndView("/ErrorPage", "model", model);
    	}
    	
    	try
    	{
    		// if the add was success or no
    		int rezult= dalDriver.add(usr);
    		if (rezult == -1) // sanitize error for preexisting user..
    		{
        		ErrorDetailedInfo paramExceptionDesc = new ErrorDetailedInfo();
        		paramExceptionDesc.setHasException(true);
        		paramExceptionDesc.setExceptionString("This user already exists in database");
        		model.addAttribute("ErrOut", paramExceptionDesc);
        		return new ModelAndView("/ErrorPage", "model", model);
    		}
    	}
    	catch (CarShareGenericException sqlSideException)
    	{
			ArrayList<String> listOfParamErrors = new ArrayList<String>();
			listOfParamErrors.add(sqlSideException.getMessage());
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setErrors(listOfParamErrors );
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	catch (Exception sqlException)
    	{
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setExceptionString(sqlException.toString());
    		errorDescModel.setHasException(true);
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	
    	
    	// after user add, log the user in..
    	try {
    		Authentication authResult = new UsernamePasswordAuthenticationToken(
    				usr.getUsrid(), usr.getPassword());
    		Authentication success = authenticationManager.authenticate(authResult);
    		SecurityContextHolder.getContext().setAuthentication(success );
    	} catch (AuthenticationException authExcept) {
    		ArrayList<String> listOfParamErrors = new ArrayList<String>();
    		listOfParamErrors.add("Failed Login...");
    		
    		ErrorDetailedInfo errorDescModel = new ErrorDetailedInfo();
    		errorDescModel.setErrors(listOfParamErrors);
    		errorDescModel.setExceptionString(authExcept.toString());
    		errorDescModel.setHasException(true);
    		model.addAttribute("ErrOut", errorDescModel);
    		
    		return new ModelAndView("/ErrorPage", "model", model);
    	}
    	
    	CalendarUtilities.ProcessTimezoneForLogin(request, genericLog, usr, "");
    	
    	Map<String, Object> pageData = new HashMap<String, Object>();
        
    	pageData.put("user", usr);
        
        // feedback workstuffs.
    	List<User_Record> feedbackHistory = this.dalDriver.getFeedBackForUser(request.getUserPrincipal().getName());
    	
    	int positiveRate = 0, negativeRate = 0, neutralRate = 0;
		
		// okay count history return to add additional attributes.
		for (int i=0; i < feedbackHistory.size(); i++)
		{
			// neutral positive and negative
			if (feedbackHistory.get(i).getFeedback()  > 0) positiveRate++; 
			else if (feedbackHistory.get(i).getFeedback() == 0)
				neutralRate++;
			else negativeRate++;
		}
		
		pageData.put("positiveRate", positiveRate);
		pageData.put("negativeRate", negativeRate);
		pageData.put("neutralRate", neutralRate);
	    pageData.put("feedbackHistory", feedbackHistory);

        
        MailerThreadObject threadMailer = new MailerThreadObject(usr.getEmail(), 
        		"Welcome to Carshare\r\n", 
        		"Welcome " + usr.getUsrid() + "\r\n " +
        				"Create or share a trip with carshare now.\r\n" +
        				"  Sincerely,\r\n The CarshareTeam\r\n",
        		"<html><body>Welcome <span>" + usr.getUsrid() + "</span> <p> Create or share a trip with carshare now.</p>  Sincerely,<br> The CarshareTeam </body></html>", genericLog);
        
        threadMailer.start();
        
        // display user profile for logged in user
        return new ModelAndView("myprofile", "model", pageData);

    }
}
