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

import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.enums.APPLICANT_UW_TYPE;
import com.stepsoln.core.db.party.Address;

import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services

def eform
Map<String, EFormAnswers> eforms;
List<Lookup> lookups;
Case currentCase;
Services services;

def prefillForm() {
	prefillOtherOwnerData()
}

def prefillOtherOwnerData(){
	Applicant otherOwner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);
	eform.put("owner_first_name", otherOwner.getFirstName());
	eform.put("owner_middle_name", otherOwner.getMiddleName());
	eform.put("owner_last_name", otherOwner.getLastName());
}

def saveData() {
	saveOtherOwnerData()
}

def saveOtherOwnerData(){
	Applicant otherOwner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);
	if (otherOwner==null) {
		otherOwner=new Applicant();
		currentCase.getApplicants().add(otherOwner);
		otherOwner.setApplicantType(APPLICANT_TYPE.POLICY_OWNER);
		otherOwner.setApplicationCase(currentCase);
		logger.debug("Creating new instance Other Owner");
	}
	else {
		logger.debug("Updating existing Other Owner instance");
	}

	otherOwner.setFirstName(eform.get("owner_first_name"));
	otherOwner.setMiddleName(eform.get("owner_middle_name"));
	otherOwner.setLastName(eform.get("owner_last_name"));
	otherOwner.setUwType(APPLICANT_UW_TYPE.NON_RISK)

	Address address = otherOwner.getFirstAddress();
	address.setType(1);
	String addressLine = eform.get("owner_address");
	if (addressLine == null || addressLine.empty)
	{
		address.setAddressLine1("N/A");
	}
	else 
	{
		address.setAddressLine1(eform.get("owner_address"));
	}
	address.setCity(eform.get("owner_city"));
	address.setState(eform.get("owner_state"));
	address.setZip(eform.get("owner_zip"));
	address.setValidated(false);
	services.hibernateTemplate.update(currentCase);

	otherOwner.getAddresses().add(address);
	otherOwner.setPrimaryAddress(address);

	services.hibernateTemplate.saveOrUpdate(otherOwner);
}