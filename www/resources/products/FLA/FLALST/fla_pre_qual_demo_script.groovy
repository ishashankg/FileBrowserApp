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
	primaryApplicant.setTobaccoUsage(Boolean.valueOf("Y".equalsIgnoreCase(eform.get("smoker_ind"))));
	services.hibernateTemplate.saveOrUpdate(primaryApplicant);
}
