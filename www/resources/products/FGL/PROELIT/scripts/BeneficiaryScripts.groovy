package PERFPRO.scripts;

import java.util.List;
import com.stepsoln.core.db.cases.Case
import org.hibernate.criterion.Restrictions;
import com.stepsoln.core.db.services.util.scripting.Services
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.db.policy.AbstractDepBen.BENEFICIARY_TYPE;
import com.stepsoln.core.db.services.EformToEntityConverter;

Case currentCase
Services services

def saveBeneficiary()
{
	def applicants=[];
	EformToEntityConverter eformEntityConverter = services.coreServices.caseService.getEformEntityConverter(currentCase);
	List<Object> applicantList = (List<Object>) eformEntityConverter.convertEformBeanToEntity(eform);
	applicants.addAll(applicantList);
	services.coreServices.caseService.applicationDataService.mergeAndSaveCaseBeneficiary(applicants,currentCase,BENEFICIARY_TYPE.PRIMARY);
}
