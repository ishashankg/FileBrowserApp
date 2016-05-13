package forms.FLALST;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.util.StringUtil
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*
import com.stepsoln.core.db.cases.Case;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.eform.EFormAnswers

def eform
Map<String, EFormAnswers> eforms;
List<Lookup> lookups;
Case currentCase;
Services services;

def prefillData() {
	prefillApplicantData()
	prefillReplacedCompany()
}

def prefillApplicantData(Applicant primaryApplicant) {
	eform.put("ins_first_name", primaryApplicant.getFirstName());
	eform.put("ins_middle_name", primaryApplicant.getMiddleName());
	eform.put("ins_last_name", primaryApplicant.getLastName());
	eform.put("ins_dob", DateUtil.formatDateSlashNullAllowed(primaryApplicant.getBirthdate()));
	eform.put("ins_gender", primaryApplicant.getGender());
	eform.put("ins_age", DateUtil.calculateAge(primaryApplicant.getBirthdate()));
	eform.put("legal_residence_address", primaryApplicant.primaryAddress.addressLine1);
	eform.put("state", primaryApplicant.primaryAddress.state);
	eform.put("city", primaryApplicant.primaryAddress.city);
	eform.put("zipcode", primaryApplicant.primaryAddress.zip);
	eform.put("preferred_phone", primaryApplicant.phone);
}

def prefillApplicantData() {
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	if (currentCase.contractLocale in ["FL"]) 
	{
		prefillApplicantData(primaryApplicant);
	}
	else if (currentCase.contractLocale in ["IN"]) 
	{
		eform.put("exist_ins_name", primaryApplicant.getFirstName() + " " +  primaryApplicant.getLastName());
		eform.put("prop_ins_name", primaryApplicant.getFirstName() + " " +  primaryApplicant.getLastName());
	}
	else if (currentCase.contractLocale in ["OK"])
	{
		def today = DateUtil.formatDateSlash(new Date());
		if (eform.get("rNotify")==null)
		{
			eform.put("rNotify", "Y");
			eform.put("applicant_date", today);
			eform.put("applicant_name", primaryApplicant.getFirstName() + " " +  primaryApplicant.getLastName());
		}
		else if (eform.get("rNotify") == "N")
		{
			eform.put("applicant_date_n", today);
			eform.put("applicant_name_n", primaryApplicant.getFirstName() + " " +  primaryApplicant.getLastName());
		}
	}
	else if (currentCase.contractLocale in ["SD"]) 
	{
		eform.put("ins_first_name", primaryApplicant.getFirstName());
		eform.put("ins_middle_name", primaryApplicant.getMiddleName());
		eform.put("ins_last_name", primaryApplicant.getLastName());
		eform.put("legal_residence_address", primaryApplicant.primaryAddress.addressLine1);
		eform.put("state", primaryApplicant.primaryAddress.state);
		eform.put("city", primaryApplicant.primaryAddress.city);
		eform.put("zipcode", primaryApplicant.primaryAddress.zip);
		eform.put("preferred_phone", primaryApplicant.phone);
	}
	else if (currentCase.contractLocale in ["WV", "RI"]) 
	{
		eform.put("insured_name", primaryApplicant.getFirstName() + " " + primaryApplicant.getLastName())
		eform.put("insured_dob", DateUtil.formatDateSlashNullAllowed(primaryApplicant.getBirthdate()))
		eform.put("insured_address", primaryApplicant.primaryAddress.addressLine1)
		eform.put("insured_citystate", primaryApplicant.primaryAddress.city + ", " + primaryApplicant.primaryAddress.state)
	}
}

def prefillReplacedCompany() {
	Applicant primaryApplicant = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	String fullName = StringUtil.defaultIfEmpty(primaryApplicant.getFirstName(), "");
	String lastName = StringUtil.defaultIfEmpty(primaryApplicant.getLastName(), "");
	fullName += (!fullName.isEmpty() && !lastName.isEmpty() ? " " + lastName : lastName);

	def otherCoverage = eforms.OTHER_COVERAGE;
	String otherInsuranceCompany = otherCoverage.get("oth_ins_company");
	String isReplacement = otherCoverage.get("oth_ins_replace");
	String otherInsStreet = otherCoverage.get("oth_ins_street");
	String otherInsCity = otherCoverage.get("oth_ins_city");
	String otherInsState = otherCoverage.get("oth_ins_state");
	String otherInsZip = otherCoverage.get("oth_ins_zip");
	String otherInsFace = otherCoverage.get("oth_ins_face");
	String otherInsYear = otherCoverage.get("oth_ins_year");

	if(StringUtils.isNotBlank(isReplacement) && isReplacement.equalsIgnoreCase("Y")) {
		eform.put("replaced_carrier___0", otherInsuranceCompany);
		eform.put("replaced_address___0", StringUtil.defaultIfEmpty(otherInsStreet,"") + ", " +
				StringUtil.defaultIfEmpty(otherInsCity,"") + ", " +
				StringUtil.defaultIfEmpty(otherInsState,"") + " " +
				StringUtil.defaultIfEmpty(otherInsZip,""));
		eform.put("replaced_faceamount___0", otherInsFace);
		eform.put("replaced_insured___0", fullName);
	}

	int i = 0;
	int j = 1;
	while (otherInsuranceCompany != null) {
		otherInsuranceCompany = otherCoverage.get("oth_ins_company2___" + i);
		isReplacement = otherCoverage.get("oth_ins_replace2___" + i);
		otherInsStreet = otherCoverage.get("oth_ins_street2___" + i);
		otherInsCity = otherCoverage.get("oth_ins_city2___" + i);
		otherInsState = otherCoverage.get("oth_ins_state2___" + i);
		otherInsZip = otherCoverage.get("oth_ins_zip2___" + i);
		otherInsFace = otherCoverage.get("oth_ins_face2___" + i);
		otherInsYear = otherCoverage.get("oth_ins_year2___" + i);
		if(StringUtils.isNotBlank(isReplacement) && isReplacement.equalsIgnoreCase("Y")) {
			eform.put("replaced_carrier___" + j, otherInsuranceCompany);
			eform.put("replaced_address___" + j, StringUtil.defaultIfEmpty(otherInsStreet,"") + ", " +
					StringUtil.defaultIfEmpty(otherInsCity,"") + ", " +
					StringUtil.defaultIfEmpty(otherInsState,"") + " " +
					StringUtil.defaultIfEmpty(otherInsZip,""));
			eform.put("replaced_faceamount___" + j, otherInsFace);
			eform.put("replaced_insured___" + j, fullName);
			j++;
		}
		i++;
	}
}
