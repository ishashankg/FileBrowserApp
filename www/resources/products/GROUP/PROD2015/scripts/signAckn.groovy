package GROUP.scripts;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
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
Case currentCase;
Services services;

/**
 * This method will be called from efrom xml.
 *
 * @return
 */
def saveData() {
	resolveSignatures();
}

/**
 * This method resolves the signature signature if ack_sig_insured is selected.
 *
 * @return
 */
def resolveSignatures() {
	services.coreServices.getRequirementsService().reloadCaseRequirements(currentCase)
	if (eformFieldBooleanValue('ack_sig_insured')) {
		List<CaseRequirement> signatureCaseRequirements = getSignatureCaseRequirements(currentCase.getRequirements())
		for(CaseRequirement requirement : signatureCaseRequirements){
			services.coreServices.getRequirementsService().signRequirement(requirement, DateUtil.convertDate(eform['ack_date_insured']), eform['ack_city'], eform['ack_state'])
		}
	}
	services.coreServices.getRequirementsService().reloadCaseRequirements(currentCase)
}

/**
 * Returns the value of given eform field.
 *
 * @param fieldName
 * @return
 */
def boolean eformFieldBooleanValue(String fieldName) {
	return EFormBeanHelper.getBooleanValueAnswer(eform[fieldName])
}

/**
 * Returns all signature requirements from given requirements.
 *
 * @return
 */
def List<CaseRequirement> getSignatureCaseRequirements(List<CaseRequirement> caseRequirements){
	List<CaseRequirement> signatureCaseRequirements = new ArrayList<CaseRequirement>()
	for(CaseRequirement caseRequirement : caseRequirements){
		if(caseRequirement.isSignatureRequired()){
			signatureCaseRequirements.add(caseRequirement)
		}
	}
	return signatureCaseRequirements;
}