package HFA.scripts;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*;
import com.stepsoln.core.db.policy.*;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.db.enums.APPLICANT_UW_TYPE;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.eapp.db.*;
import com.stepsoln.eapp.db.party.*;
import com.stepsoln.core.db.services.util.scripting.Services;
import com.stepsoln.core.db.services.RequirementsService;
import com.stepsoln.core.eform.EFormBeanHelper;
import com.stepsoln.core.db.enums.REQUIREMENT_TYPE;

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

//called from efrom xml
def saveData() {
	//System.out.println("onSave groovy event handler invoked");
	resolveSignatures();
}

def resolveSignatures() {
	boolean isReload = false;
	//resolve signature for primary insured
	Applicant primaryInsured = currentCase.getApplicant(APPLICANT_TYPE.PRIMARY_INSURED);
	
	if(primaryInsured != null) {		
		if (eformFieldBooleanValue('hipaa_sig_insured')) {						
			CaseRequirement hipSigRequirement = RequirementsService.findCaseRequirementByTargetAndCode(currentCase.getRequirements(), CaseRequirement.CASE_REQUIREMENT_TARGET.PRIMARY_INSURED, REQUIREMENT_TYPE.ESIGNATURE, "HIP");
			if(hipSigRequirement != null )
			{
				services.coreServices.getRequirementsService().signRequirement(hipSigRequirement, DateUtil.convertDate(eform['hipaa_date_insured']), eform['contract_city'], eform['contract_state']);
				isReload = true;
			}
		}
		
		if (eformFieldBooleanValue('ack_sig_insured')) {
			CaseRequirement ackSigRequirement = RequirementsService.findCaseRequirementByTargetAndCode(currentCase.getRequirements(), CaseRequirement.CASE_REQUIREMENT_TARGET.PRIMARY_INSURED, REQUIREMENT_TYPE.ESIGNATURE, "ACK");
			if(ackSigRequirement != null)
			{
				services.coreServices.getRequirementsService().signRequirement(ackSigRequirement, DateUtil.convertDate(eform['ack_date_insured']), eform['ack_city'], eform['ack_state']);
				isReload = true;
			}
		}
		
	}
	
	//resolve signature for primary insured
	Applicant policyOwner = currentCase.getApplicant(APPLICANT_TYPE.POLICY_OWNER);
	
	if(policyOwner != null) {
		if (eformFieldBooleanValue('hipaa_sig_owner')) {
			CaseRequirement hipSigRequirement = RequirementsService.findCaseRequirementByTargetAndCode(currentCase.getRequirements(), CaseRequirement.CASE_REQUIREMENT_TARGET.POLICY_OWNER, REQUIREMENT_TYPE.ESIGNATURE, "HIP");
			if(hipSigRequirement != null )
			{
				services.coreServices.getRequirementsService().signRequirement(hipSigRequirement, DateUtil.convertDate(eform['hipaa_date_owner']), eform['contract_city'], eform['contract_state']);
				isReload = true;
			}
		}
		
		if (eformFieldBooleanValue('ack_sig_owner')) {			
			CaseRequirement ackSigRequirement = RequirementsService.findCaseRequirementByTargetAndCode(currentCase.getRequirements(), CaseRequirement.CASE_REQUIREMENT_TARGET.POLICY_OWNER, REQUIREMENT_TYPE.ESIGNATURE, "ACK");
			if(ackSigRequirement != null )
			{
				services.coreServices.getRequirementsService().signRequirement(ackSigRequirement, DateUtil.convertDate(eform['ack_date_owner']), eform['ack_city'], eform['ack_state']);
				isReload = true;
			}
		}
	}
	if(isReload)
	{
		services.coreServices.getRequirementsService().reloadCaseRequirements(currentCase);
	}
	
}

def boolean eformFieldBooleanValue(String fieldName) {
	
	return EFormBeanHelper.getBooleanValueAnswer(eform[fieldName])
	
}

