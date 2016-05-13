package forms.FLALST;

import com.stepsoln.core.db.cases.Beneficiary;
import java.text.SimpleDateFormat;
import java.util.Set;
import org.slf4j.Logger;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.db.cases.*;
import com.stepsoln.core.db.policy.*;
import com.stepsoln.core.db.services.util.AutoCompleteLookupUtility;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.eapp.db.*;
import com.stepsoln.eapp.db.party.*;
import com.stepsoln.core.db.services.util.scripting.Services;
import com.stepsoln.core.db.cases.CaseRequirement;
import com.stepsoln.core.db.enums.REQUIREMENT_GROUP;
import com.stepsoln.core.db.enums.REQUIREMENT_TYPE;
import com.stepsoln.core.db.cases.CaseRequirement.STATUS;
import com.stepsoln.core.db.services.EformToEntityConverter;

def eform
Case currentCase
Services services

def saveBeneficiaries() {
	try {
		def eFormBeneficiaries = [];
		eFormBeneficiaries = findBeneficiary();
		mergeAndSaveCaseBeneficiaries(eFormBeneficiaries);
	}
	catch (Exception e) {
		logger.error("Failed to execute saveBeneficiaries script!", e);
	}
}

def prefillForm() {
	loadBeneficiaries()
}

def loadBeneficiaries() {
	eform.put("bene_grouping1", eforms.APPLICANT.bene_grouping);
	
	eform.put("bene_first_name1", eforms.APPLICANT.bene_first_name);
	eform.put("bene_middle_name1", eforms.APPLICANT.bene_middle_name);
	eform.put("bene_last_name1", eforms.APPLICANT.bene_last_name);
	
	eform.put("bene_org_name1", eforms.APPLICANT.bene_org_name);
}

def findBeneficiary() {
	def bens=[];
	// Primary beneficiary
	grouping = eform.get("bene_grouping1");
	if (grouping!=null && grouping.equals("I"))
	{
		def Beneficiary ben = new Beneficiary();
		
		firstName = eform.get("bene_first_name1");
		middleName = eform.get("bene_middle_name1");
		lastName = eform.get("bene_last_name1");
			
		ben.setFirstName(firstName);
		ben.setMiddleName(middleName);
		ben.setLastName(lastName);
		
		fullName = firstName != null ? firstName : "";
		fullName += middleName != null ? " " + middleName : "";
		fullName += lastName != null ? " " + lastName : "";
		ben.setFullName(fullName);
		
		ben.setPercentage(eform.get("bene_percent1"));
		ben.setRelationship(eform.get("bene_relation1"));
		ben.setGovernmentId(eform.get("bene_ssn1"));
		ben.setBeneficiaryType(AbstractDepBen.BENEFICIARY_TYPE.PRIMARY);
		bens.add(ben);
	} 
	else if (grouping!=null && grouping.equals("O"))
	{
		def Beneficiary ben = new Beneficiary();
		ben.setFullName(eform.get("bene_org_name1"));
		ben.setPercentage(eform.get("bene_percent1"));
		ben.setRelationship(eform.get("bene_org_relation1"));
		ben.setGovernmentId(eform.get("bene_tax_id1"));
		ben.setBeneficiaryType(AbstractDepBen.BENEFICIARY_TYPE.PRIMARY);
		bens.add(ben);
	}
	
	// Other beneficiary
	for (i in 0..5) 
	{
		grouping = eform.get("bene_grouping2___" + i);
		beneficiaryType = "P".equalsIgnoreCase(eform.get("beneficiary_type2___" + i)) ? AbstractDepBen.BENEFICIARY_TYPE.PRIMARY : AbstractDepBen.BENEFICIARY_TYPE.CONTINGENT;
		if (grouping!=null && grouping.equals("I"))
		{
			def Beneficiary ben = new Beneficiary();
			
			firstName = eform.get("bene_first_name2___" + i);
			middleName = eform.get("bene_middle_name2___" + i);
			lastName = eform.get("bene_last_name2___" + i);
			
			ben.setFirstName(firstName);
			ben.setMiddleName(middleName);
			ben.setLastName(lastName);
			
			fullName = firstName != null ? firstName : "";
			fullName += middleName != null ? " " + middleName : "";
			fullName += lastName != null ? " " + lastName : "";
			ben.setFullName(fullName);
			
			ben.setPercentage(eform.get("bene_percent2___" + i));
			ben.setRelationship(eform.get("bene_relation2___" + i));
			ben.setGovernmentId(eform.get("bene_ssn2___" + i));
			ben.setBeneficiaryType(beneficiaryType);
			bens.add(ben);
		}
		else if (grouping!=null && grouping.equals("O"))
		{
			def Beneficiary ben = new Beneficiary();
			ben.setFullName(eform.get("bene_org_name2___" + i));
			ben.setPercentage(eform.get("bene_percent2___" + i));
			ben.setRelationship(eform.get("bene_org_relation2___" + i));
			ben.setGovernmentId(eform.get("bene_tax_id2___" + i));
			ben.setBeneficiaryType(beneficiaryType);
			bens.add(ben);
		}
	}

	return bens;
}

def mergeAndSaveCaseBeneficiaries(eFormBeneficiaries)
{
	logger.debug("Saving " + eFormBeneficiaries.size() + " new bens");
	oldBens = services.hibernateTemplate.getAll(Beneficiary.class, "select b from " + Beneficiary.class.getCanonicalName() + " as b where b.applicationCase = ?", currentCase);
	logger.debug("Removing " + oldBens.size() + " old bens");
	oldBens.each { services.hibernateTemplate.delete(it);};
	bens = new ArrayList<Beneficiary>();
	for(Beneficiary ben: eFormBeneficiaries)
	{
		if (null!=ben)
		{
			ben.setApplicationCase(currentCase);
			services.hibernateTemplate.save(ben);
			bens.add(ben);
			logger.debug("Ben " + ben.getId() + " saved.");
		}
	}
	currentCase.setBeneficiaries(bens);
}
