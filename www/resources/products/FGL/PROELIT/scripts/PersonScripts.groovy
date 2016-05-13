package PLATINA.scripts;

import java.util.List;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.DetachedCriteria;
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.db.cases.Case
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

def preFilleForm(){
	calculateAge();
}

def calculateAge()
{
	if(currentCase.primaryApplicant.birthdate != null)
	{
		eform["age"] = DateUtil.calculateAge(currentCase.primaryApplicant.birthdate); 
	}
	
}

def saveOwner()
{
	def applicants=[];
	EformToEntityConverter eformEntityConverter = services.coreServices.caseService.getEformEntityConverter(currentCase);
	List<Object> applicantList = (List<Object>) eformEntityConverter.convertEformBeanToEntity(eform);
	applicants.addAll(applicantList);
	services.coreServices.caseService.applicationDataService.mergeAndSaveCaseApplicant(applicants,currentCase,APPLICANT_TYPE.POLICY_OWNER);
}

def saveAnnuitant()
{
	def applicants=[];
	EformToEntityConverter eformEntityConverter = services.coreServices.caseService.getEformEntityConverter(currentCase);
	List<Object> applicantList = (List<Object>) eformEntityConverter.convertEformBeanToEntity(eform);
	applicants.addAll(applicantList);
	services.coreServices.caseService.applicationDataService.mergeAndSaveCaseApplicant(applicants,currentCase,APPLICANT_TYPE.ANNUITANT);
}


















