package PLATINA.scripts;

import java.util.List;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.DetachedCriteria;
import com.stepsoln.core.db.services.util.scripting.Services;
import com.stepsoln.core.db.cases.Case;
import com.stepsoln.core.db.cases.CaseAgent;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.cases.CaseRequirement.CASE_REQUIREMENT_TARGET;
import com.stepsoln.core.db.cases.CaseRequirement.STATUS;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.enums.APPLICANT_UW_TYPE
import com.stepsoln.core.db.security.SecurityInvitation;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.services.EformToEntityConverter;

def eform
Case currentCase
Services services


def saveAuthorizationDetails()
{
	if (StringUtils.isNotBlank(eform["signed_at_state"]))
	{
		currentCase.setContractLocale(eform["signed_at_state"]);
	}
	def caseAgents=[];
	EformToEntityConverter eformEntityConverter = services.coreServices.caseService.getEformEntityConverter(currentCase);
	List<Object> agentList = (List<Object>) eformEntityConverter.convertEformBeanToEntity(eform);
	caseAgents.addAll(agentList);
	List<CaseAgent> newAgents = services.coreServices.caseAgentService.saveCaseAgentForCase(agentList, currentCase);
	currentCase.setCaseAgents(newAgents);
}


def preFillForm()
{
	setAgentCommission();
}

def setAgentCommission()
{
	if (StringUtils.isBlank(eform["commission_split"]))
	{
		eform["commission_split"] = "100";
	}
}














