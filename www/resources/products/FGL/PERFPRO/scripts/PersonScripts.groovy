package PERFPRO.scripts;

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils

import com.stepsoln.core.db.cases.Applicant
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.db.services.EformToEntityConverter
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.util.DateUtil

import java.util.stream.Collectors

def eform
Case currentCase
Services services

def preFilleForm()
{
	calculateAge();
}

def calculateAge()
{
	if (currentCase.primaryApplicant.birthdate != null)
	{
		eform["age"] = DateUtil.calculateAge(currentCase.primaryApplicant.birthdate); 
	}	
}

def saveOwner()
{
	def contractStateCheck = eform["contract_state_check"]
	if ((StringUtils.isNotBlank(eform["state"])) && ("Y".equalsIgnoreCase(contractStateCheck)))
	{
		currentCase.setContractLocale(eform["state"]);
	}
	else if (StringUtils.isNotBlank(eform["contract_state"]))
	{
		currentCase.setContractLocale(eform["contract_state"]);
	}
	else
	{
		currentCase.setContractLocale(null);
	}
	def ownerAnnuitant = eform["owner_Annuitant"];
	if (!("Y".equalsIgnoreCase(ownerAnnuitant)))
	{
		List<APPLICANT_TYPE> applicantTypeList = [APPLICANT_TYPE.ANNUITANT, APPLICANT_TYPE.JOINT_ANNUITANT];
		delete(applicantTypeList);
	}
	def jointOwner = eform["joint_owner"];
	if (!("Y".equalsIgnoreCase(jointOwner)))
	{
		List<APPLICANT_TYPE> applicantTypeList = [APPLICANT_TYPE.JOINT_OWNER];
		delete(applicantTypeList);
	}
	save(ownerAnnuitant);
}

def saveAnnuitant()
{
	def applicants = [];
	def jointAnnuitant = eform["joint_annuitant"];
	if (!("Y".equalsIgnoreCase(jointAnnuitant)))
	{
		List<APPLICANT_TYPE> applicantTypeList = [APPLICANT_TYPE.JOINT_ANNUITANT];
		delete(applicantTypeList);
	}
	save("Y");
}

def save(String ownerAnnuitant)
{
	EformToEntityConverter eformEntityConverter = services.coreServices.caseService.getEformEntityConverter(currentCase);
	List<Object> applicantList = (List<Object>) eformEntityConverter.convertEformBeanToEntity(eform);
	List<APPLICANT_TYPE> applicantTypeList = [APPLICANT_TYPE.POLICY_OWNER];
	List<Applicant> applicants = applicantList.stream().filter({
		applicant -> applicantTypeList.stream().anyMatch({a -> applicant.getApplicantType().equals(a)})}).collect(Collectors.toList());
	applicants.each 
	{
		applicant -> 
		if (!("Y".equalsIgnoreCase(ownerAnnuitant))) 
		{
			applicant.setIsPrimary(true);
		}
	};
	if (CollectionUtils.isNotEmpty(applicantList))
	{
		services.coreServices.caseService.applicationDataService.mergeAndSaveCaseApplicant(applicantList, currentCase);
	}
}

def delete(List<APPLICANT_TYPE> applicantTypeList)
{
	List<Applicant> applicants = currentCase.getApplicants(applicantTypeList);
	services.coreServices.caseService.applicationDataService.removeAssociatedRequirements(currentCase.getCaseId(), applicants);
	services.coreServices.caseService.applicationDataService.deleteApplicants(applicants);
}
