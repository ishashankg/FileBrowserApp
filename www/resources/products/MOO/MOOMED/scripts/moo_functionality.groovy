package MOOMED.scripts

import org.apache.commons.lang.StringUtils
import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.config.ConfigResource
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.db.party.Agent
import com.stepsoln.core.db.security.SecurityInvitation
import com.stepsoln.core.db.services.CoreServices
import com.stepsoln.core.util.QuickMap
import com.stepsoln.core.util.TemplateHelper
import com.stepsoln.servicebus.api.EmailRequestType

def String applicantResidenceState;
def Case currentCase;
def CoreServices coreServices;
	
def Boolean isUniSexRates()
{
	if (applicantResidenceState.equalsIgnoreCase("MT"))
	{
		return true;
	}
	return false;
}


def String getCoverageOptionCode()
{
	return optionCode;
}

def APPLICANT_TYPE defaultApplicantType()
{
	return APPLICANT_TYPE.PRIMARY_INSURED
}


def List generateEmailContentForSendApplicantForReview()
{
	List retList = new ArrayList();
	Applicant primaryApplicant = currentCase.getPrimaryApplicant();
	if(primaryApplicant != null)
	{
		EmailRequestType emailContent = generateEmailContentFor(primaryApplicant);
		if(emailContent != null)
		{
			retList.add(emailContent);
		}
	}
	
	Applicant policyOwner = currentCase.getApplicant(com.stepsoln.core.db.enums.APPLICANT_TYPE.POLICY_OWNER);
	if(policyOwner != null)
	{
		EmailRequestType emailContent = generateEmailContentFor(policyOwner);
		if(emailContent != null)
		{
			retList.add(emailContent);
		}
	}
	
	return retList;
}

def EmailRequestType generateEmailContentFor(Applicant app)
{
	EmailRequestType emailRequest = null;
	try{
	
	//Long agentId = currentCase.getAgentId();
	//Agent agent = coreServices.getCaseService().getHibernateTemplate().get(Agent.class, agentId);

	ConfigResource resource = coreServices.getConfigurationRepository().loadLastestResource(currentCase.getCarrierId(), currentCase.getProductId(), "mooMed_agent_send_case_to_applicant_for_review");
	if(resource != null && app != null && !StringUtils.isEmpty(app.getEmail()))
	{
		emailRequest = new EmailRequestType();
		SecurityInvitation invitation = coreServices.getInvitationService().createSecurityInvitation(currentCase.getCaseId(), app.getId(), "&nextViewName=SecurityInvitation.applicantESignatureFlowStartAction", null, 30);
		String emailContent = TemplateHelper.velocityTranslateString(new String(resource.getContent()),
			new QuickMap("policyNumber", currentCase.getPolicyNo())
				.add("applicantFirstName",app.getFirstName())
				.add("applicantLastName",app.getLastName())
				.add("inviteLink",invitation.getInvitationUrl())
				);
		//emailRequest.setAddressFrom(agent.getEmail());
		emailRequest.setAddressTo(app.getEmail());
		emailRequest.setBodyContent(emailContent);
		emailRequest.setSubject("Your Life Insurance Application with MooMed");
	}
	}catch(Exception e)
	{
		e.printStackTrace();
	}
	return emailRequest;
}
