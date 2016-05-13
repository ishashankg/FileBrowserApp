package HFA.scripts;

import com.stepsoln.core.db.enums.REQUIREMENT_TYPE
import org.hibernate.criterion.DetachedCriteria
import org.hibernate.criterion.Restrictions
import com.stepsoln.core.db.agency.Agency
import com.stepsoln.core.db.agency.AgentInfo
import com.stepsoln.core.db.agency.ProducerLicense
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.party.Agent
import com.stepsoln.core.db.policy.*
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.DetachedCriteria;
import java.util.Date;
import com.stepsoln.core.db.services.RequirementsService;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.eform.EFormBeanHelper;

def eform
Case currentCase;
Services services;
Agent agent;

def prefillForm()
{
	prefillAgentInfo();
}

def prefillAgentInfo()
{
	if(!currentCase.getIsPaperCase())
	{
		agent = currentCase.getAgent();
		if (agent!=null)
		{
			prefillAgentName();
			if (currentCase.getContractLocale().equals("FL") && eform.get("agent_license_no")==null)
			{
				prefillAgentLicense();
			}
			else
			{
				prefillAgentID();
			}
		}
		else
		{
			logger.error("No Agent Found");
		}
	}
	
}

def prefillAgentName()
{
	 eform.put("printed_name", agent.getFullName());
}

def prefillAgentLicense()
{
	String license_num = getLicenseNum();
	eform.put("agent_license_no", license_num);
}

def prefillAgentID()
{
	eform.put("agent_id", agent.getAgentCode());
}

def String getLicenseNum()
{
	ProducerLicense license = getAgentLicense();
	if (license!=null){
		return license.getLicenseNumber();
	}
	logger.warn("No License for this Agent. Checking Agency licenses.");
	
	license = getAgencyLicense();
	if (license!=null){
		return license.getLicenseNumber();
	}
	logger.error("No Agent or Agency License found");
}

def ProducerLicense getAgentLicense(){
	DetachedCriteria licCriteria = DetachedCriteria.forClass(ProducerLicense.class);
	licCriteria.add(Restrictions.eq("agent", agent));
	licCriteria.add(Restrictions.eq("stateProvince", currentCase.getContractLocale()));
	
	return services.hibernateTemplate.findFirstOrNull(licCriteria);
}

def ProducerLicense getAgencyLicense(){
	DetachedCriteria agCriteria = DetachedCriteria.forClass(AgentInfo.class);
	agCriteria.add(Restrictions.eq("agent", agent));
	List<AgentInfo> agInfos = services.hibernateTemplate.getAll(agCriteria);
	for (agInfo in agInfos){
		Agency agency = agInfo.getAgency();
		DetachedCriteria licCriteria = DetachedCriteria.forClass(ProducerLicense.class);
		licCriteria.add(Restrictions.eq("agency", agency));
		licCriteria.add(Restrictions.eq("stateProvince", currentCase.getContractLocale()));
		ProducerLicense license = services.hibernateTemplate.findFirstOrNull(licCriteria);
		if (license!=null){
			logger.debug("Agency License found");
			return license;
		}
	}
}


def saveData()
{
	if(currentCase.getIsPaperCase())
	{
		resolveAgentSignatureRequirement();
		updateAgent()
	}
}


def boolean resolveAgentSignatureRequirement()
{
	if(eformFieldBooleanValue("agent_sig"))
	{
		CaseRequirement agentSigRequirement = RequirementsService.findCaseRequirementByTargetAndCode(currentCase.getRequirements(), CaseRequirement.CASE_REQUIREMENT_TARGET.AGENT, REQUIREMENT_TYPE.ESIGNATURE, "AAT");
		if(agentSigRequirement != null && agentSigRequirement.getStatus() != CaseRequirement.STATUS.CANCELLED)
		{
			Date signDate = DateUtil.convertDate(eform['agent_sig_date']);
			if(signDate == null)
			{
				signDate = new Date();
			}
			services.coreServices.getRequirementsService().signRequirement(agentSigRequirement, signDate, " ", " ");
		}
	}
}

def updateAgent()
{
	if(eform.get("agent_id_lookup")==null)
	{
		if (currentCase.getAgent()!=null)
		{
			currentCase.setAgent(null);
			currentCase.setAgentId(null);
			services.hibernateTemplate.update(currentCase);
			logger.debug("Agent Removed from case");
		}
	}
	else
	{
		if (isNewAgent())
		{
			Long agentId = Long.valueOf(eform.get("agent_id_db"));
			newAgent = services.hibernateTemplate.get(Agent.class, agentId);
			currentCase.setAgent(newAgent);
			currentCase.setAgentId(agentId);
			services.hibernateTemplate.update(currentCase);
			logger.debug("New Agent saved");
		}
	}
}

def boolean isNewAgent()
{
	return currentCase.getAgent()==null || (currentCase.getAgent().getId()!=null && currentCase.getAgent().getId()!=eform.get("agent_id_db"))
}

def boolean eformFieldBooleanValue(String fieldName) {
	return EFormBeanHelper.getBooleanValueAnswer(eform[fieldName])
}