package forms.FLALST;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import com.stepsoln.core.util.DateUtil;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.*
import com.stepsoln.core.db.policy.*

import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.util.DateUtil;

import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.EFormAnswers;
import com.stepsoln.core.db.services.*
import com.stepsoln.eapp.db.*
import com.stepsoln.eapp.db.party.*
import com.stepsoln.core.db.services.util.scripting.Services;
import org.apache.commons.beanutils.PropertyUtils;

def eform
Map<String, EFormAnswers> eforms;
List<Lookup> lookups;
Case currentCase;
Services services;

def initializePage() {
	eform.put("ins_date", DateUtil.formatDateSlash(new Date()));
}

def saveSignatureInfo(){
	//Get the requirements we need to update
	def sigReq = currentCase.getRequirements().find{it.getType() == REQUIREMENT_TYPE.ESIGNATURE;}

	sigReq.setSignatureCity(eform.get("ins_city"));
	sigReq.setSignatureState(eform.get("ins_state"));
	sigReq.setSignatureStatus(CaseRequirement.SIGNATURE_STATUS.SIGNED);
	if(eform.get("ins_date") != null)
	{
		sigReq.setSignatureDate(DateUtil.SLASH_FORMAT.parse(eform.get("ins_date")));
	}
	sigReq.setStatus(CaseRequirement.STATUS.RESOLVED);
	sigReq.setResolvedDate(new Date());
	services.hibernateTemplate.update(sigReq);
}
