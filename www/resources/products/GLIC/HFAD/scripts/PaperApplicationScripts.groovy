import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import java.util.Map;
import com.stepsoln.core.db.cases.*;
import com.stepsoln.core.db.cases.Case.CASE_SOURCE;
import com.stepsoln.core.db.services.util.scripting.Services;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.core.db.party.Agent;
import com.stepsoln.core.db.agency.ProducerLicense;
import com.stepsoln.core.db.agency.CarrierProducerAppointment;
import com.stepsoln.core.db.agency.ProducerLicense.LICENSE_STATUS;
import com.stepsoln.core.db.security.SecurityUser;
import com.stepsoln.core.db.Lookup;
import java.util.Arrays;

Case currentCase;
Services services;
def eform;
	
def formLoad()
{
	System.out.println("hello");
}

def formNew()
{
	System.out.println("new form");
}

def pageOneEnter()
{
	System.out.println("page one enter");
}

def transformPaperApplication()
{
	logger.debug("Start transform paper application");
}

def savePaperForm()
{
	setCaseDescription();
	setAgentInformation();
}
	
def setAgeofApplicant()
{
	if (eform['date_of_birth']) {
		eform['age'] = DateUtil.calculateAge(DateUtil.convertDate(eform['date_of_birth']))
	}
}

def setExternalCaseNumber()
{
	logger.debug("Generate new External case number for paper flow");
	if (currentCase.getCaseSource()==Case.CASE_SOURCE.IPAPER || currentCase.getCaseSource()==Case.CASE_SOURCE.EPAPER)
	{
		String num = currentCase.getExternalCaseNumber();
		if (num.startsWith("00"))
		{
			if (currentCase.getCaseSource()==Case.CASE_SOURCE.IPAPER)
			{
				num = "64" + num.substring(2);
			}		
			if (currentCase.getCaseSource()==Case.CASE_SOURCE.EPAPER)
			{
				num = "65" + num.substring(2);
			}
			currentCase.setExternalCaseNumber(num);
			currentCase.setPolicyNo(num);
			services.hibernateTemplate.update(currentCase);
			logger.debug("Generated External case number for paper flow: " + num);
		}
	} 

}

def setCaseDescription()
{
	logger.debug("sets applicant name to case description for paper flow");
	if (currentCase.getCaseSource() == Case.CASE_SOURCE.IPAPER || currentCase.getCaseSource() == Case.CASE_SOURCE.EPAPER)
	{
		fName = eform.get("first_name");
		lName = eform.get("last_name");
		currentCase.setDescription(lName + ", " + fName);
		services.hibernateTemplate.update(currentCase);
	}
}

def setAgentInformation()
{
	if (currentCase.getCaseSource() == Case.CASE_SOURCE.EPAPER)
	{
		agentCode = (eform.get("agent_id")?:eform.get("agent_license_no"));
		printedName = eform.get("printed_name");
		agentLicState = eform.get("agent_license_state").toUpperCase();
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(SecurityUser.class);
		detachedCriteria.add(Restrictions.eq("username", agentCode));
		def List<SecurityUser> securityUserList = services.hibernateTemplate.getAll(detachedCriteria);
		if (securityUserList.isEmpty())
		{
			eform.put("agent_present", "N");
		}
		else
		{
			eform.put("agent_present", "Y");
			def SecurityUser securityUser = securityUserList.get(0);
			def Long agentId = securityUser.getAgentId();
			def Agent agent = services.hibernateTemplate.get(Agent.class, agentId);
			if (!(agent.getFullName().equalsIgnoreCase(printedName)))
			{
				eform.put("agent_name_mismatch", "Y");
			}
			else
			{
				eform.put("agent_name_mismatch", "N");
				detachedCriteria = DetachedCriteria.forClass(Lookup.class);
				detachedCriteria.add(Restrictions.eq("context", "step"));
				detachedCriteria.add(Restrictions.eq("name", "LOCALE_STATE"));
				detachedCriteria.add(Restrictions.or(Restrictions.eq("code", agentLicState), Restrictions.eq("description", agentLicState)));
				def List<Lookup> lookupList = services.hibernateTemplate.getAll(detachedCriteria);
				if (lookupList.isEmpty())
				{
					eform.put("state_present", "N");
				}
				else
				{
					eform.put("state_present", "Y");
					def String[] nonCompactStates = ["AZ","AR","DC","CA","CT","DE","FL","MT","NY","ND","SD"];
					def List<String> nonCompactStatesList = Arrays.asList(nonCompactStates);
					detachedCriteria = DetachedCriteria.forClass(CarrierProducerAppointment.class, "cpa");
					detachedCriteria.createAlias("license", "lic", DetachedCriteria.LEFT_JOIN);
					detachedCriteria.add(Restrictions.eq("cpa.carrierId", currentCase.getCarrierId()));
					detachedCriteria.add(Restrictions.eq("cpa.productId", currentCase.getProductId()));
					detachedCriteria.add(Restrictions.eq("cpa.agent.id", agentId));
					detachedCriteria.add(Restrictions.eq("lic.licenseStatus", ProducerLicense.LICENSE_STATUS.ACTIVE));
					detachedCriteria.add(Restrictions.eq("lic.stateProvince", lookupList.get(0).getCode()));
					detachedCriteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
					def List<CarrierProducerAppointment> cpaList = services.hibernateTemplate.getAll(detachedCriteria);
					if (cpaList.isEmpty())
					{
						eform.put("agent_lic_state_valid", "N");
					}
					else
					{
						eform.put("agent_lic_state_valid", "Y");
						if ((nonCompactStatesList.contains(currentCase.getContractLocale().toUpperCase())) || ((nonCompactStatesList.contains(lookupList.get(0).getCode().toUpperCase()))))
						{
							if (currentCase.getContractLocale().toUpperCase().equalsIgnoreCase(lookupList.get(0).getCode().toUpperCase()))
							{
								eform.put("agent_lic_state_valid_non_compact", "Y");
							}
							else
							{
								eform.put("agent_lic_state_valid_non_compact", "N");
							}
						}
						else
						{
							eform.put("agent_lic_state_valid_non_compact", "Y");
						}
					}
				}
			}
		}
	}
}
