package forms.FLALST;

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
import com.stepsoln.core.db.agency.ProducerLicense;

import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services;

def eform
Map<String, EFormAnswers> eforms;
List<Lookup> lookups;
Case currentCase;
Services services;


def prefillForm(){
	prefillApplicantData()
}

def prefillApplicantData(){
	Applicant primaryApplicant = currentCase.getPrimaryApplicant();
	eform.put("ins_first_name", primaryApplicant.getFirstName());
	eform.put("ins_middle_name", primaryApplicant.getMiddleName());
	eform.put("ins_last_name", primaryApplicant.getLastName());
	
	Address secondary = primaryApplicant.getAddressByType(31);
	if(secondary != null){
		eform.put("addressee_addl_address", secondary.getAddressLine1());
		eform.put("addressee_addl_address2", secondary.getAddressLine2());
		eform.put("addressee_addl_city", secondary.getCity());
		eform.put("addressee_addl_state", secondary.getState());
		eform.put("addressee_addl_zip", secondary.getZip());
	}

	Address additional =  primaryApplicant.getAddressByType(17);
	if(additional != null){
		eform.put("ins_addl_address", additional.getAddressLine1());
		eform.put("ins_addl_address2", additional.getAddressLine2());
		eform.put("ins_addl_city", additional.getCity());
		eform.put("ins_addl_state", additional.getState());
		eform.put("ins_addl_zip", additional.getZip());
	}
}

def saveAddresses(){
	System.out.println("In the saveAddresses() function.");
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	Address secondary = (primaryApplicant.getAddressByType(31) == null) ? new Address() : primaryApplicant.getAddressByType(31);
	Address additional =  (primaryApplicant.getAddressByType(17) == null) ? new Address() : primaryApplicant.getAddressByType(17);
	boolean updated = false;
	
	//StringUtils.isNotBlank(eform.get("addressee_first_name")) && StringUtils.isNotBlank(eform.get("addressee_middle_name")) && StringUtils.isNotBlank(eform.get("addressee_last_name"))
	if(StringUtils.isNotBlank(eform.get("addressee_addl_address")) || StringUtils.isNotBlank(eform.get("addressee_addl_address2")) || StringUtils.isNotBlank(eform.get("addressee_addl_city")) || StringUtils.isNotBlank(eform.get("addressee_addl_state")) || StringUtils.isNotBlank(eform.get("addressee_addl_zip"))){
		secondary.setAddressLine1(eform.get("addressee_addl_address"));
		secondary.setAddressLine2(eform.get("addressee_addl_address2"));
		secondary.setCity(eform.get("addressee_addl_city"));
		secondary.setState(eform.get("addressee_addl_state"));
		secondary.setZip(eform.get("addressee_addl_zip"));
		secondary.setType(31);
		
		primaryApplicant.saveOrUpdateAddressByType(secondary);
		updated = true;
		System.out.println("Saved secondary address.");
	}
	
	if(StringUtils.isNotBlank(eform.get("ins_addl_address")) || StringUtils.isNotBlank(eform.get("ins_addl_address2")) || StringUtils.isNotBlank(eform.get("ins_addl_city")) || StringUtils.isNotBlank(eform.get("ins_addl_state"))){
		additional.setAddressLine1(eform.get("ins_addl_address"));
		additional.setAddressLine2(eform.get("ins_addl_address2"));
		additional.setCity(eform.get("ins_addl_city"));
		additional.setState(eform.get("ins_addl_state"));
		additional.setZip(eform.get("ins_addl_zip"));
		additional.setType(17);
		
		primaryApplicant.saveOrUpdateAddressByType(additional);
		updated = true;
		System.out.println("Saved additional address.");
	}
	
	if(updated){
		services.hibernateTemplate.saveOrUpdate(primaryApplicant);
		System.out.println("Saved updated addresses.");
	}
	
	System.out.println("End the saveAddresses() function.");
}
