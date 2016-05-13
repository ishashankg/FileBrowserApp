package forms.FLALST;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*

import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.party.Address;

import com.stepsoln.core.eform.EForm;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services

def eform
List<Lookup> lookups;
Case currentCase;
Services services;


def prefillForm() {
	prefillApplicantData()
//	System.out.println("prefillForm groovy event handler invoked");
}

def prefillApplicantData(){
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	eform.put("ins_first_name", primaryApplicant.getFirstName());
	eform.put("ins_middle_name", primaryApplicant.getMiddleName());
	eform.put("ins_last_name", primaryApplicant.getLastName());
	eform.put("ins_date_of_birth", DateUtil.formatDateSlashNullAllowed(primaryApplicant.getBirthdate()));
	eform.put("ins_gender", primaryApplicant.getGender());
//	eform.put("height_feet", primaryApplicant);			TODO
//	eform.put("height_inches", primaryApplicant);
//	eform.put("weight_pounds", primaryApplicant);
}


def saveData() {
//	System.out.println("saveData groovy event handler attempted");
	saveApplicantData()
//	System.out.println("saveData groovy event handler invoked");
}

def saveApplicantData(){
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	if (primaryApplicant==null) {
		primaryApplicant=new Applicant();
		currentCase.getApplicants().add(primaryApplicant);
		primaryApplicant.setApplicantType(APPLICANT_TYPE.PRIMARY_INSURED);
		primaryApplicant.setIsPrimary(true);
		primaryApplicant.setApplicationCase(currentCase);
		logger.debug("Creating new instance Primary Applicant");
	}
	else {
		logger.debug("Updating existing Primary Applicant instance");
	}
	primaryApplicant.setFirstName(eform.get("ins_first_name"));
	primaryApplicant.setMiddleName(eform.get("ins_middle_name"));
	primaryApplicant.setLastName(eform.get("ins_last_name"));
	
	System.out.println("Date of Birth " + eform.get("ins_date_of_birth"));
	
	if(StringUtils.isNotEmpty(eform.get("ins_date_of_birth"))){
		primaryApplicant.setBirthdate(DateUtil.convertDate(eform.get("ins_date_of_birth")));
	}
	if(StringUtils.isNotEmpty(String.valueOf(eform.get("ins_gender")))) {
		primaryApplicant.setGender(Integer.valueOf(eform.get("ins_gender")));
	}
//	primaryApplicant.set(eform.get("height_feet"));				//TODO
//	primaryApplicant.set(eform.get("height_inches"));
//	primaryApplicant.set(eform.get("weight_pounds"));
	
	services.hibernateTemplate.saveOrUpdate(primaryApplicant);
	
}
