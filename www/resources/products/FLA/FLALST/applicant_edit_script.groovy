package forms.FLALST;

import java.util.List;
import java.util.Map;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*

import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE
import com.stepsoln.core.db.enums.PHONE_TYPE
import com.stepsoln.core.db.party.Address;

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
	prefillApplicantData()
}

def prefillApplicantData(){
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	if (primaryApplicant)
	{
		eform.put("ins_first_name", primaryApplicant.firstName);
		eform.put("ins_middle_name", primaryApplicant.middleName);
		eform.put("ins_last_name", primaryApplicant.lastName);
		eform.put("ins_date_of_birth", DateUtil.formatDateSlashNullAllowed(primaryApplicant.birthdate));
		eform.put("ins_gender", primaryApplicant.gender);
		eform.put("height_feet", eforms.PRE_QUAL_APP.height_feet);			
		eform.put("height_inches", eforms.PRE_QUAL_APP.height_inches);
		eform.put("weight_pounds", eforms.PRE_QUAL_APP.weight_pounds);
		eform.put("email_addr", primaryApplicant.email);
		
		if( primaryApplicant.primaryAddress)
		{
			eform.put("legal_residence_address", primaryApplicant.primaryAddress.addressLine1);
			eform.put("legal_residence_address_line2", primaryApplicant.primaryAddress.addressLine2);
			eform.put("city", primaryApplicant.primaryAddress.city);
			eform.put("state", primaryApplicant.primaryAddress.state);
			eform.put("zipcode", primaryApplicant.primaryAddress.zip);
		}
	}

	def benegroup = eforms.APPLICANT.bene_grouping;
	if (benegroup!=null)
	{
		eform.put("bene_grouping", benegroup);
	}
	else
	{
		eform.put("bene_grouping", "I");
	}

	if(eform.get("policy_eff_date").equals(null)){
		eform.put("policy_eff_date", DateUtil.formatTimeQuick(DateUtil.convertDate(System.currentTimeMillis() + DateUtil.MILISECONDS_PER_DAY)));
	}
}


def saveData() {
	saveApplicantData()
}


def saveApplicantData(){
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	if (primaryApplicant==null) {
		primaryApplicant=new Applicant();
		currentCase.getApplicants().add(primaryApplicant);
		primaryApplicant.setApplicantType(APPLICANT_TYPE.PRIMARY_INSURED);
		primaryApplicant.setApplicationCase(currentCase);
		logger.debug("Creating new instance Primary Applicant");
	}
	else {
		logger.debug("Updating existing Primary Applicant instance");
	}
	
	Address address = primaryApplicant.getFirstAddress();
	address.setType(1);
	address.setAddressLine1(eform.get("legal_residence_address"));
	address.setAddressLine2(eform.get("legal_residence_address_line2"));
	address.setCity(eform.get("city"));
	address.setState(eform.get("state"));
	address.setZip(eform.get("zipcode"));
	address.setValidated(false);
	currentCase.setContractLocale(eform.get("state")); //set the contract state to match the residency state
	services.hibernateTemplate.update(currentCase);

	primaryApplicant.getAddresses().add(address);
	primaryApplicant.setPrimaryAddress(address);
	
	primaryApplicant.setPhone(eform.get("preferred_phone"));
	primaryApplicant.setPhoneType(PHONE_TYPE.GENERAL)
	primaryApplicant.setPhoneOptIn(Boolean.FALSE)
	primaryApplicant.setSecondaryPhone(eform.get("alternate_phone"));
	primaryApplicant.setSecondaryPhoneType(PHONE_TYPE.GENERAL)
	primaryApplicant.setSecondaryPhoneOptIn(Boolean.FALSE)
	primaryApplicant.setGovernmentId(eform.get("social_security"));
	primaryApplicant.setEmail(eform.get("email_addr"));

	//TODO ? eform.get("secondary_addressee");
	
	//Beneficiary Information
	Beneficiary primaryBeneficiary = (currentCase.getPrimaryBeneficiary() != null) ? currentCase.getPrimaryBeneficiary() : new Beneficiary();
	primaryBeneficiary.setFirstName(eform.get("bene_first_name"));
	primaryBeneficiary.setMiddleName(eform.get("bene_middle_name"));
	primaryBeneficiary.setLastName(eform.get("bene_last_name"));
	primaryBeneficiary.setApplicationCase(currentCase);
	services.hibernateTemplate.saveOrUpdate(primaryBeneficiary);
		
	services.hibernateTemplate.saveOrUpdate(primaryApplicant);
	
}