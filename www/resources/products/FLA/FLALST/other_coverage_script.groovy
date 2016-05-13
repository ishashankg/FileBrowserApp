package forms.FLALST;

import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils

import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.services.util.scripting.Services

def eform
Case currentCase;
Services services;

def saveData() {
	saveOtherCoverages()
}

def saveOtherCoverages() {
	removeExistingCoverages()
	String companyName = eform.get("oth_ins_company");
	ArrayList<CaseExistingInsurance> otherInsuranceCoverages = new ArrayList<CaseExistingInsurance>();
	
	if (StringUtils.isNotBlank(companyName)) 
	{
		CaseExistingInsurance otherInsurance = new CaseExistingInsurance();
		Applicant applicant = currentCase.getPrimaryApplicant();
		
		otherInsurance.setNaicCompanyName(companyName);
		otherInsurance.setNaicCompanyNumber(eform.get("oth_ins_naic"));
		otherInsurance.setMailingStreet(eform.get("oth_ins_street"));
		otherInsurance.setMailingCity(eform.get("oth_ins_city"));
		otherInsurance.setMailingState(eform.get("oth_ins_state"));
		otherInsurance.setMailingZipCode(eform.get("oth_ins_zip"));
		//for the time being we won't save the issued year, see #7821
	
		String faceAmtStr = eform.get("oth_ins_face");
		if (StringUtils.isNotBlank(faceAmtStr))
		{
			BigDecimal faceAmt = new BigDecimal(Integer.parseInt(faceAmtStr));
			otherInsurance.setCoverageAmount(faceAmt);
		}
		
		String isReplacement = eform.get("oth_ins_replace");
		if (StringUtils.isNotBlank(isReplacement))
		{
			int replaceInt = isReplacement.equalsIgnoreCase("Y") ? 1 : 0;
			otherInsurance.setIsReplacement(replaceInt);
		}
		
		otherInsurance.setCase(currentCase);
		services.hibernateTemplate.saveOrUpdate(otherInsurance);
		otherInsuranceCoverages.add(otherInsurance);
	}
		
	//check for a grand total of 5 additional coverages -> 0, 1, 2, 3 plus the one checked from above
	int i = 0;
	while (companyName != null)
	{
		companyName = eform.get("oth_ins_company2___" + i)
		if (StringUtils.isNotBlank(companyName))
		{
			CaseExistingInsurance otherInsurance = new CaseExistingInsurance();
			 
			otherInsurance.setNaicCompanyName(companyName);
			otherInsurance.setNaicCompanyNumber(eform.get("oth_ins_naic2___" + i));
			otherInsurance.setMailingStreet(eform.get("oth_ins_street2___" + i));
			otherInsurance.setMailingCity(eform.get("oth_ins_city2___" + i));
			otherInsurance.setMailingState(eform.get("oth_ins_state2___" + i));
			otherInsurance.setMailingZipCode(eform.get("oth_ins_zip2___" + i));
			
			//for the time being we won't save the issued year, see #7821
			
			String insuranceFaceAmtStr = eform.get("oth_ins_face2___" + i);
			if (StringUtils.isNotBlank(insuranceFaceAmtStr))
			{
				BigDecimal insuranceFaceAmt = new BigDecimal(Integer.parseInt(insuranceFaceAmtStr));
				otherInsurance.setCoverageAmount(insuranceFaceAmt);
			}
			
			String rep = eform.get("oth_ins_replace2___" + i);
			if (StringUtils.isNotBlank(rep))
			{
				int repInt = rep.equalsIgnoreCase("Y") ? 1 : 0;
				otherInsurance.setIsReplacement(repInt);
			}

			otherInsurance.setCase(currentCase);
			services.hibernateTemplate.saveOrUpdate(otherInsurance);
			otherInsuranceCoverages.add(otherInsurance);
		}
		i++;
	}
	
	if (CollectionUtils.isNotEmpty(otherInsuranceCoverages)) 
	{
		currentCase.setExistingInsurance(otherCoverages);
		services.hibernateTemplate.saveOrUpdate(currentCase);
	}
}

def removeExistingCoverages() 
{
	List<CaseExistingInsurance> oldInsurances = currentCase.getExistingInsuranceList();
	if (CollectionUtils.isNotEmpty(oldInsurances)) 
	{
		oldInsurances.each { services.hibernateTemplate.delete(it); }
	}
}
