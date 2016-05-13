package ULIFE.scripts

import com.stepsoln.core.db.enums.APPLICANT_TYPE;

def String applicantResidenceState;
	
def Boolean isUniSexRates()
{
	if (applicantResidenceState.equalsIgnoreCase("MT"))
	{
		return true;
	}
	return false;
}

def String getCoverageOptionCode()
{
	return optionCode;
}

def APPLICANT_TYPE defaultApplicantType()
{
	return APPLICANT_TYPE.PRIMARY_INSURED
}