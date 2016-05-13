package HFA.scripts;

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