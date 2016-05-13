package HFA.scripts;

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

def formLoad() {
	System.out.println("formLoad groovy event handler invoked");
}

def formNew() {
	System.out.println("formNew groovy event handler invoked");
}

def pageOneEnter() {
	System.out.println("pageOneEnter groovy event handler invoked");
}

def importData() {
	importOtherOwner();
}


def importOtherOwner() {
	logger.debug("Start importing Other Owner for case id="+currentCase.getCaseId()+" hash="+currentCase.hashCode());
	
		Applicant owner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);
		if (owner==null) {
			owner=new Applicant();
			currentCase.getApplicants().add(owner);
			owner.setApplicantType(APPLICANT_TYPE.POLICY_OWNER);
			owner.setApplicationCase(currentCase);
			logger.debug("Creating new instance Other Owner");
		} else {
			logger.debug("Updating existing Other Owner instance");
		}
		Address address = new Address();
		address.setType(1);
		owner.setFirstName(eform.get("owner_first_name"));
		owner.setLastName(eform.get("owner_last_name"));
		owner.setMiddleName(eform.get("owner_middle_name"));
		owner.setGovernmentId(eform.get("owner_social_security_number"));
		owner.setBirthdate(DateUtil.convertDate(eform.get("owner_date_of_birth")));
		owner.setEmail(eform.get("owner_email_address"));
		address.setAddressLine1(eform.get("owner_residence_address"));
		address.setCity(eform.get("owner_city"));
		address.setState(eform.get("owner_residence_state"));
		address.setZip(eform.get("owner_zip"));
		address.setValidated(false);
		List<Address> addresses = new ArrayList<Address>();
		addresses.add(address);
		owner.setAddresses(addresses);
		owner.setPrimaryAddress(address);
		services.hibernateTemplate.saveOrUpdate(owner);

	logger.debug("End importing Other Owner");
}

def prefillForm() {
	prefillPrimaryApplicantData();
	prefillOtherOwner();
}

def prefillPrimaryApplicantData() {
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);

	eform.put("ins_first_name", primaryApplicant.getFirstName());
	eform.put("ins_middle_name", primaryApplicant.getMiddleName());
	eform.put("ins_last_name", primaryApplicant.getLastName());

}

def prefillOtherOwner() {
	Applicant owner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);

	if (owner!=null)
	{
		eform.put("owner_first_name", owner.getFirstName());
		eform.put("owner_middle_name", owner.getMiddleName());
		eform.put("owner_last_name", owner.getLastName());
		eform.put("owner_social_security_number",owner.getGovernmentId());
		if (owner.getBirthdate()!=null)
		{
			eform.put("owner_date_of_birth", DateUtil.formatTimeQuick(owner.getBirthdate()));
		}
		eform.put("owner_email_address",owner.getEmail());
		if (owner.getPrimaryAddress()!=null)
		{
			eform.put("owner_residence_address",owner.getPrimaryAddress().getAddressLine1()+"->"+owner.getPrimaryAddress().getState());
			eform.put("owner_city",owner.getPrimaryAddress().getCity());
			eform.put("owner_residence_state",owner.getPrimaryAddress().getState());
			eform.put("owner_zip",owner.getPrimaryAddress().getZip());
		}
	}
} 

def fillSsn() {
	if(eform['owner_social_security_number']){
	eform["auth_pin_owner"]= eform["owner_social_security_number"].substring(7, 11)
	}
}