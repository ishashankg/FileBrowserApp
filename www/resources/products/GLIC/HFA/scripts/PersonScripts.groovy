package HFA.scripts;

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

def importData()
{
	if (null!=currentCase.primaryApplicant)
	{
		if(currentCase.primaryApplicant.primaryAddress != null)
		{
			currentCase.primaryApplicant.primaryAddress.ensureRequiredFieldsNotNull();
		}
		services.coreServices.caseService.applicationDataService.saveCaseApplicant(currentCase.primaryApplicant);
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
		removeAllApplicants(APPLICANT_TYPE.THIRD_PARTY_DESIGNEE)
		addSecondaryAddress();
	}
	else if (eform["secondary_ind_fl"]=="N" || eform["secondary_ind_seniors"]=="N" || eform["secondary_ind_seniors_fl"]=="N")
	{
		removeAllApplicants(APPLICANT_TYPE.THIRD_PARTY_DESIGNEE)
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
			if (req.getRequirementCode().equalsIgnoreCase("OWN") && !req.getStatus().equals(STATUS.CANCELLED))
			{
				req.setStatus(STATUS.CANCELLED);
				services.hibernateTemplate.update(req);
			}
		}
		if (reqToRemove.size()>0)
		{
			currentCase.getRequirements().removeAll(reqToRemove);
			services.hibernateTemplate.deleteAll(reqToRemove);
		}
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SecurityInvitation.class);
		detachedCriteria.add(Restrictions.eq("applicantId", owner.getId()));
		List<SecurityInvitation> invitations = services.hibernateTemplate.getAll(detachedCriteria);
		if (invitations.size()>0)
		{
			services.hibernateTemplate.deleteAll(invitations);
		}
		currentCase.getApplicants().remove(owner);
		services.hibernateTemplate.delete(owner);
	}
}

def removeAllApplicants(APPLICANT_TYPE appType)
{
	if (appType == null)
	{
		return
	}
	def allToBeRemoved = services.hibernateTemplate.getAll(Applicant.class, "select a from " + Applicant.class.getCanonicalName() + " as a where a.applicationCase = ? and a.applicantType = ?", currentCase, appType);
	allToBeRemoved.each { it ->
		services.hibernateTemplate.delete(it);
	}
}

def addSecondaryAddress()
{
	Applicant secondaryAddressee = new Applicant();
	secondaryAddressee.setApplicationCase(currentCase);
	secondaryAddressee.setApplicantType(APPLICANT_TYPE.THIRD_PARTY_DESIGNEE)
	secondaryAddressee.setUwType(APPLICANT_UW_TYPE.NOT_APPLICABLE)
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
	services.hibernateTemplate.update(currentCase);
	services.hibernateTemplate.saveOrUpdate(secondaryAddressee);
}





















