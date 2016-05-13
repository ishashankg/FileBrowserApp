package forms.FLALST;

import com.stepsoln.core.db.uw.CaseUwRiskCalc;
import java.util.List;
import com.stepsoln.core.db.cases.ApplicationQuote;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.DetachedCriteria;
import com.stepsoln.core.db.cases.Case;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.db.party.Agent;
import com.stepsoln.core.db.agency.Agency;
import com.stepsoln.core.db.agency.AgentInfo;
import com.stepsoln.core.db.agency.ProducerLicense;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.db.cases.ApplicationQuoteItem
import org.apache.commons.beanutils.PropertyUtils
import com.stepsoln.core.db.product.PlanOption.OPTION_VALUE_TYPE
import java.math.BigDecimal
import com.stepsoln.core.db.product.ProductPlanAvailability

def eform
Map<String, EFormAnswers> eforms;
List<Lookup> lookups;
Case currentCase;
Services services;
Agent agent;


def prefillForm(){
//	prefillApplicantFaceAmount()
	prefillAgentData()
}

def prefillApplicantFaceAmount()
{
	List<Long> applicationQuoteIds = services.hibernateTemplate.find(Long.class,
					"select min(applicationQuoteId) from " + ApplicationQuote.class.getCanonicalName() + " where caseInfo.caseId = ? and finalFlag = ?", currentCase.getCaseId(), Boolean.FALSE);
	if (!(applicationQuoteIds.isEmpty()) && applicationQuoteIds.get(0)!=null)
	{
		ApplicationQuote applicationQuote = services.hibernateTemplate.getWithProxies(ApplicationQuote.class, applicationQuoteIds.get(0), "applicationQuoteItems");
		for (ApplicationQuoteItem applicationQuoteItem: applicationQuote.getApplicationQuoteItems())
		{
			ProductPlanAvailability productPlanAvailability = services.hibernateTemplate.getWithProxies(ProductPlanAvailability.class, applicationQuoteItem.getPlanAvailabilityId(), "planOption");
			if (OPTION_VALUE_TYPE.FACE_AMOUNT == productPlanAvailability.getPlanOption().getValueType())
			{
				if (applicationQuoteItem.getLevelValue().subtract(new BigDecimal(500)).compareTo(BigDecimal.ZERO) > 0)
				{
					eform.put("ag_applicant_addtl_face", "Y");
					eform.put("ag_applicant_addtl_face_amt", applicationQuoteItem.getCoverageAmount().intValue());
				}
				else
				{
					eform.put("ag_applicant_addtl_face", "N");
					eform.put("ag_applicant_addtl_face_amt", 0);
				}
			}
		}
	}
}

def prefillAgentData(){
	agent = currentCase.getAgent();
	if(agent!=null){
		logger.info("Displaying Agent Information");
		eform.put("ag_first_name", agent.getFirstName());
		eform.put("ag_last_name", agent.getLastName());
		eform.put("ag_id", agent.getAgentCode());
		//TODO eform.put("ga_id", );
		eform.put("ag_email", agent.getEmail());
		eform.put("ag_sig_date", DateUtil.formatDateFormal(new Date()));
		if (agent.getOfficePhone()!=null){
			eform.put("ag_contact", agent.getOfficePhone());
		}
		else{
			eform.put("ag_contact", agent.getMobilePhone());
		}
		if(currentCase.getContractLocale()==null){
			logger.info("No Contract Locale for this case - can't get Agent License without State");
			eform.put("st_license_num", null);
		}
		else{
			prefillAgentLicense();		
		}
	}
	else{
		logger.info("No Agent Found");
		eform.put("ag_first_name", null);
		eform.put("ag_last_name", null);
		eform.put("ag_id", null);
		eform.put("ag_email", null);
		eform.put("ag_contact", null);
		eform.put("st_license_num", null);
	}
}

def prefillAgentLicense(){
	
	ProducerLicense license = getAgentLicense();
	String license_num;
	if (license==null){
		logger.info("No License for this Agent. Checking Agency licenses.");
		license = getAgencyLicense();
	}
	if (license!=null){
		license_num = license.getLicenseNumber();
	}
	else{
		license_num=null;
	}
	eform.put("st_license_num", license_num);
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
			logger.info("Agency License found");
			return license;
		}
	}
	logger.error("Agency License not found");
}