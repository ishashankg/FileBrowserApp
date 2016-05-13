package forms.FLAST.scripts;

import org.apache.camel.ProducerTemplate
import org.slf4j.Logger
import com.stepsoln.core.db.carrier.Carrier
import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.party.Agent
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.cases.CaseRequirement.LOCK_STATUS;
import com.stepsoln.core.db.config.ConfigResource
import com.stepsoln.core.db.security.SecurityInvitation
import com.stepsoln.core.db.security.SecurityInvitation.INVITATION_STATUS
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.util.QuickMap
import com.stepsoln.core.util.RandomUtil
import com.stepsoln.core.util.SystemConfiguration
import com.stepsoln.core.util.TemplateHelper
import com.stepsoln.core.util.XMLUtil
import com.stepsoln.core.util.StepXMLHelper
import com.stepsoln.servicebus.api.EmailRequestType
import com.stepsoln.servicebus.api.ParameterType;
import com.stepsoln.servicebus.api.StepRequestType
import com.stepsoln.servicebus.api.ObjectFactory
import org.apache.commons.lang.StringUtils
import com.stepsoln.core.db.cases.ApplicationQuote
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import org.apache.commons.beanutils.PropertyUtils
import com.stepsoln.core.db.product.PlanOption.OPTION_VALUE_TYPE
import java.math.BigDecimal
import java.util.Date
import com.stepsoln.core.db.security.SecurityUser
import com.stepsoln.core.db.enums.REQUIREMENT_TYPE
import com.stepsoln.core.db.product.ProductPlanAvailability
import com.stepsoln.core.util.modeladapters.CaseToNbMessageAdapter
import com.stepsoln.servicebus.api.HeaderVariable
import com.stepsoln.core.util.EncryptionUtils;

def Case currentCase;
def SecurityUser securityUser;
def ApplicationQuote applicationQuote;
def Logger logger;
def Services services;
def CoreServices coreServices;
def ProducerTemplate producerTemplate;
def Map<String, Map<String, Object>> eformsAnswers;


def void postNBServiceAddCase()
{
	//Only send the invitation email to case that does not have an agent id
	if(currentCase.getAgentId() == null)
	{
		SecurityInvitation invitation = createSecurityInvitation();
		Applicant app = currentCase.getPrimaryApplicant();
		
		ConfigResource resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "fla_email_post_addcase_to_applicant");
		if(resource != null && app.getEmail() != null)
		{
			String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()), new QuickMap("inviteLink",invitation.getInvitationUrl()).add("applicantFirstName", app.getFirstName()).add("applicantLastName", app.getLastName()).add("policyNumber", currentCase.getPolicyNo()));
			// "direct:stepMailer"
			sendEmail(resource, emailContent, "FidelityLife@stepsolutions.com", app.getEmail(), "Your Life Insurance Application with Fidelity Life Association", "activemq://GENERIC_EMAIL.QUEUE");
			
		}
	}
	//Sets HOAssignedAppNumber and TrackingID in outbound 103
	currentCase.setExternalCaseNumber(trackingId);
	
	processApplicationQuote();
}

def void processApplicationQuote()
{
	ApplicationQuote newApplicationQuote = new ApplicationQuote()
	PropertyUtils.copyProperties(newApplicationQuote, applicationQuote);
	applicationQuote.setFinalFlag(Boolean.FALSE);
	newApplicationQuote.setApplicationQuoteId(null);
	newApplicationQuote.setCaseInfo(currentCase);
	newApplicationQuote.setApplicationQuoteItems(new HashSet<ApplicationQuoteItem>(0));
	for (ApplicationQuoteItem applicationQuoteItem : applicationQuote.getApplicationQuoteItems())
	{
		ApplicationQuoteItem newApplicationQuoteItem = new ApplicationQuoteItem();
		PropertyUtils.copyProperties(newApplicationQuoteItem, applicationQuoteItem);
		ProductPlanAvailability productPlanAvailability = services.hibernateTemplate.getWithProxies(ProductPlanAvailability.class, newApplicationQuoteItem.getPlanAvailabilityId(), "planOption");
		newApplicationQuoteItem.setProductPlanAvailability(productPlanAvailability);
//		eformsAnswers.AG_ATTEST.ag_applicant_addtl_face = "N";
//		eformsAnswers.AG_ATTEST.ag_applicant_addtl_face_amt = 0;
		if (OPTION_VALUE_TYPE.FACE_AMOUNT == newApplicationQuoteItem.getProductPlanAvailability().getPlanOption().getValueType())
		{
			if (newApplicationQuoteItem.getLevelValue().subtract(new BigDecimal(500)).compareTo(new BigDecimal(0)) == 1)
			{
//				eformsAnswers.AG_ATTEST.ag_applicant_addtl_face_amt = newApplicationQuoteItem.getLevelValue().subtract(new BigDecimal(500));
//				eformsAnswers.AG_ATTEST.ag_applicant_addtl_face = "Y";
				newApplicationQuoteItem.setLevelValue(new BigDecimal(500));
				newApplicationQuoteItem.setCoverageAmount(new BigDecimal(500000));
			}
		}
		else
		{
			if (newApplicationQuoteItem.getLevelValue().subtract(new BigDecimal(500000)).compareTo(new BigDecimal(0)) == 1)
			{
				newApplicationQuoteItem.setLevelValue(new BigDecimal(500000));
				newApplicationQuoteItem.setCoverageAmount(new BigDecimal(500000));
			}
		}
		newApplicationQuoteItem.setQuoteItemId(null);
		newApplicationQuoteItem.setApplicationQuote(newApplicationQuote);
		newApplicationQuote.getApplicationQuoteItems().add(newApplicationQuoteItem);
	}
	services.hibernateTemplate.update(applicationQuote);
	coreServices.getCoverageRepository().saveQuote(currentCase, newApplicationQuote, services.hibernateTemplate);
}

def boolean sendAcquireEsignataureEmailToApplicant()
{
	//lock all the eform requirements except for FULL_HIPAA
	List<CaseRequirement> reqList = currentCase.getRequirements();
	for(CaseRequirement curReq : reqList)
	{
		if(REQUIREMENT_TYPE.EFORM.equals(curReq.getType()) && !curReq.getRequirementCode().contains("HIPAA") )	
		{
			curReq.setLockStatus(LOCK_STATUS.LOCKED);
			services.hibernateTemplate.saveOrUpdate(curReq);
		}
	}
	String tempPassword = null;
	if (securityUser != null)
	{
		tempPassword = securityUser.getPassword();
	}
	SecurityInvitation invitation = createSecurityInvitation("&nextViewName=SecurityInvitation.applicantESignatureFlowStartAction");
	Applicant app = currentCase.getPrimaryApplicant();
	
	Long agentId = currentCase.getAgentId();
	Agent agent = services.hibernateTemplate.get(Agent.class, agentId);
	String officePhone = "";
	String mobilePhone = "";
	
	if (!(StringUtils.isEmpty(agent.getOfficePhone())))
	{
		officePhone = agent.getOfficePhone();
	}
	
	if (!(StringUtils.isEmpty(agent.getMobilePhone())))
	{
		officePhone = agent.getOfficePhone();
	}
	ConfigResource resource = null;
	if (securityUser == null)
	{
		resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "fla_email_post_agent_submit");
		System.out.println("Agent submit.");
	}
	else
	{
		resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "fla_email_post_agent_submit_resend");
		System.out.println("Agent re-submit.");
	}
	if (resource != null && app.getEmail() != null)
	{
		String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()), new QuickMap("inviteLink",invitation.getInvitationUrl()).add("tempPassword", tempPassword)
				.add("applicantFirstName", app.getFirstName()).add("applicantLastName", app.getLastName()).add("policyNumber", currentCase.getPolicyNo())
				.add("agentFirstName", agent.getFirstName()).add("agentLastName", agent.getLastName()).add("agentMiddleName",agent.getMiddleName())
				.add("agentOfficePhone", officePhone).add("agentMobilePhone", mobilePhone));
		sendEmail(resource, emailContent, "FidelityLife@stepsolutions.com", app.getEmail(), "Your Life Insurance Application with Fidelity Life Association", "activemq://GENERIC_EMAIL.QUEUE");
		
	}
	currentCase.setProposedPolicyDate(new Date());
	services.hibernateTemplate.saveOrUpdate(currentCase);
}


/**
* This method sends a message to the queue to generates an application packet.
*/
def private void generateApplicationPacket()
{
   CaseToNbMessageAdapter adapter = new CaseToNbMessageAdapter();
   producerTemplate.sendBodyAndHeader("activemq://GENERATE_APPLICATION_PACKET.QUEUE", StepXMLHelper.toXML(adapter.createNBMessage(currentCase, coreServices)),HeaderVariable.REQUEST_CASE_ID.name(), currentCase.getCaseId());
}

def private SecurityInvitation createSecurityInvitation()
{
	return createSecurityInvitation(null);
}

def private SecurityInvitation createSecurityInvitation(String additionalParams)
{
	//Before we create a new invite, check for an existing one first. This may not be the first time we're trying to send an invite on this case. Ticket #8777.
	existingInvites = services.hibernateTemplate.getAll(SecurityInvitation.class, "select i from "+SecurityInvitation.class.getCanonicalName()+" as i where i.caseId=?", currentCase.getCaseId());
	SecurityInvitation invitation = null;
	useExistingInvite = false;
	existingInvites.each { 
		if(it != null && it.getStatus() != INVITATION_STATUS.CANCELLED){
			invitation = it;
			useExistingInvite = true;
		} 
	};
	
	//If there isn't an existing invite, we'll create a new one for this case.
	if(!useExistingInvite){
		System.out.println("### No existing invite was found. Creating one. ###");
		
		invitation = new SecurityInvitation();
		invitation.setStatus(INVITATION_STATUS.NEW);
		invitation.setCaseId(currentCase.getCaseId());
		invitation.setToken(RandomUtil.generateGUID());
		String url = String.format("%s/public/invite/invite.xhtml?invitationCode=%s",SystemConfiguration.current().getProperty("eappServerUrl"),invitation.getToken());
		if (securityUser != null)
		{
			invitation.setUserId(securityUser.getUserId());
			securityUser.setPassword(EncryptionUtils.hash(securityUser.getPassword(),""));
			securityUser.setIsTemporary(true);
			services.hibernateTemplate.saveOrUpdate(securityUser);
		}
		if (additionalParams != null)
		{
			url = url+additionalParams;
		}
		invitation.setInvitationUrl(url);
		services.hibernateTemplate.save(invitation);
	}
	else {
		System.out.println("### Existing invite found. Sending it. ###");
	}

	return invitation;
}

def boolean sendEmailToAgentForReview()
{
	//unlock all the eform requirements except for FULL_HIPAA
	List<CaseRequirement> reqList = currentCase.getRequirements();
	for(CaseRequirement curReq : reqList)
	{
		if(REQUIREMENT_TYPE.EFORM.equals(curReq.getType()) && !curReq.getRequirementCode().contains("HIPAA") )
		{
			curReq.setLockStatus(LOCK_STATUS.UNLOCKED);
			services.hibernateTemplate.saveOrUpdate(curReq);
		}
	}
	
	Applicant app = currentCase.getPrimaryApplicant();
	Long agentId = currentCase.getAgentId();
	Agent agent = services.hibernateTemplate.get(Agent.class, agentId);

	ConfigResource resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "fla_agent_revision_email");
	if(resource != null && agent.getEmail() != null)
	{
		String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()), new QuickMap("policyNumber", currentCase.getPolicyNo())
				.add("agentFirstName",agent.getFirstName()).add("agentLastName",agent.getLastName()));
		sendEmail(resource, emailContent, "FidelityLife@stepsolutions.com", agent.getEmail(), "Application Revision Requested", "activemq://GENERIC_EMAIL.QUEUE");
		
	}
	
	System.out.println("Email Sent To Agent For Review.");
}

def private void sendEmail(ConfigResource resource, String emailContent, String fromAddress, String toAddress, String subject, String destination)
{
	EmailRequestType email = new EmailRequestType();
	email.setAddressTo(toAddress);
	//email.setAddressFrom(SystemConfiguration.current().getProperty("step.default_email_from_address"));
	email.setAddressFrom(fromAddress);
	email.setSubject(subject);
	email.setBodyContent(emailContent);
	email.setRequirementId(0);
	if(resource.getMimeType() != null)
	{
		ParameterType msgHeader = new ParameterType();
		msgHeader.setName("Content-Type");
		msgHeader.setValue(resource.getMimeType());
		email.getMessageHeader().add(msgHeader);
	}
	
	//System.out.println(email.getBodyContent());
	
	ObjectFactory factory = new ObjectFactory();
	StepRequestType request = factory.createStepRequestType();
	request.setEmailRequest(email);
	Map<String, Object> headers = new HashMap<String, Object>();
	/*
	Carrier carrier = services.hibernateTemplate.get(Carrier.class, currentCase.getCarrierId());	
	headers.put("Carrier",carrier.getCarrierCode());
	headers.put("ServiceName", "generate.email.serviceroute");
	*/ 
	//requestBodyAndHeaders
	//	producerTemplate.requestBodyAndHeaders(destination,XMLUtil.jaxbToXML("StepRequestType",request),headers);

	producerTemplate.sendBodyAndHeaders(destination,XMLUtil.jaxbToXML("StepRequestType",request),headers);
}

def boolean resetPassword()
{	
	Applicant app = currentCase.getPrimaryApplicant();
	ConfigResource resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "fla_user_password_reset");
	if (resource != null && app.getEmail() != null)
	{
		String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()), new QuickMap("tempPassword", securityUser.getPassword()).add("applicantFirstName", app.getFirstName())
			.add("applicantLastName", app.getLastName()).add("policyNumber", currentCase.getPolicyNo()));
		sendEmail(resource, emailContent, "FidelityLife@stepsolutions.com", app.getEmail(), "Your Life Insurance Application with Fidelity Life Association", "activemq://GENERIC_EMAIL.QUEUE");
	}
	securityUser.setPassword(EncryptionUtils.hash(securityUser.getPassword(),""));
	securityUser.setIsTemporary(true);
	services.hibernateTemplate.saveOrUpdate(securityUser);
}
