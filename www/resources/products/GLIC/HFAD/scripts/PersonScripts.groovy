package HFA.scripts;

import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.db.cases.Case
import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.cases.CaseRequirement.CASE_REQUIREMENT_TARGET;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;

def eform
Case currentCase
Services services

def preFilleForm(){
	calculateAge();
	prefillSSN();
}

def calculateAge()
{
	eform["age"] = DateUtil.calculateAge(currentCase.primaryApplicant.birthdate);
}


def prefillSSN()
{
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	String id = primaryApplicant.getGovernmentId();
	eform["auth_pin_applicant"] = id.substring(id.length() - 4);
}

def importData()
{
	if (null!=currentCase.primaryApplicant)
	{
		services.hibernateTemplate.update(currentCase.primaryApplicant);
	}
	else 
	{
		logger.error("PersonScript.groovy failed to update primary applicant because current case object does not have one");
	}

	if (eform.get("oth_owner_ind")=="N")
	{
		removeOtherOwner();
	}
	
	if (eform.get("secondary_ind_fl")=="Y"||eform.get("secondary_ind_seniors")=="Y"||eform.get("secondary_ind_seniors_fl")=="Y")
	{
		println "Adding secondary Info";
		removeSecondaryAddressee();
		addSecondaryAddress();
	}
}

def removeOtherOwner()
{
	Applicant owner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);
	if (owner !=null)
	{
		
		List<CaseRequirement> requirements = currentCase.getRequirements();
		List<CaseRequirement> reqToRemove = new ArrayList<CaseRequirement>();
		for (CaseRequirement req: requirements)
		{
			if (req.getTarget()!=null && req.getTarget().equals(CASE_REQUIREMENT_TARGET.POLICY_OWNER))
			{
				reqToRemove.add(req);
			}
		}
		if (reqToRemove.size()>0)
		{
			currentCase.getRequirements().removeAll(reqToRemove);
			services.hibernateTemplate.deleteAll(reqToRemove);
		}
		currentCase.getApplicants().remove(owner);
		services.hibernateTemplate.delete(owner);
	}
}

def removeSecondaryAddressee()
{
	Applicant secondaryAddressee = currentCase.getApplicant(APPLICANT_TYPE.THIRD_PARTY_DESIGNEE);
	if (secondaryAddressee !=null)
	{
		currentCase.getApplicants().remove(secondaryAddressee);
		services.hibernateTemplate.delete(secondaryAddressee);
	}
}

def addSecondaryAddress()
{
	Applicant secondaryAddressee = new Applicant();
	secondaryAddressee.setApplicationCase(currentCase);
	secondaryAddressee.setApplicantType(APPLICANT_TYPE.THIRD_PARTY_DESIGNEE)
	String flAppend = StringUtils.isNotBlank(eform.get("fl_secondary_first_name"))?"fl_":"";
	secondaryAddressee.setFirstName(eform.get(flAppend+"secondary_first_name"));
	if (StringUtils.isNotBlank(eform.get(flAppend+"secondary_middle_name")))
	{
		secondaryAddressee.setMiddleName(eform.get(flAppend+"secondary_middle_name"));
	}
	secondaryAddressee.setLastName(eform.get(flAppend+"secondary_last_name"));

	Address secondary = new Address();
	secondary.setType(1);
	if(StringUtils.isNotBlank(eform.get(flAppend+"secondary_address_1")))
	{
		secondary.setAddressLine1(eform.get(flAppend+"secondary_address_1"));
	}
	if (StringUtils.isNotBlank(eform.get(flAppend+"secondary_address_2")))
	{
		secondary.setAddressLine2(eform.get(flAppend+"secondary_address_2"));
	}
	if (StringUtils.isNotBlank(eform.get(flAppend+"secondary_city")))
	{
		secondary.setCity(eform.get(flAppend+"secondary_city"));
	}
	if (StringUtils.isNotBlank(eform.get(flAppend+"secondary_state")))
	{
		secondary.setState(eform.get(flAppend+"secondary_state"));
	}
	String zip = eform.get(flAppend+"secondary_zip")?.toString();
	if (StringUtils.isNotBlank(zip))
	{
		secondary.setZip(zip);
	}
	secondaryAddressee.setAddresses([secondary]);
	services.hibernateTemplate.saveOrUpdate(secondaryAddressee);
}





















