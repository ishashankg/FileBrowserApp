package forms.FLALST;

import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.Date;
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
import com.stepsoln.core.util.DateUtil;

def eform
Case currentCase
Services services

def saveDependents() {
	try {
		def eFormDependents = [];
		eFormDependents = findDependent();
		mergeAndSaveCaseDependents(eFormDependents);
	}
	catch (Exception e) {
		logger.error("Failed to execute saveDependents script!", e);
	}
}

def findDependent() {
	keys = eform.keySet();
	def deps = [];

	for(Object k: keys) {
		String key = (String)k;
		String othKey = key;
		String genderKey
		String dobKey;
		if (key.startsWith("child_first_name")) {
			def depFirstName = eform.get(key);
			othKey = key.replaceFirst("first", "last");
			def depLastName = eform.get(othKey);
			othKey = key.replaceFirst("first", "middle");
			def depMiddleName = eform.get(othKey);
			def relation = "";
			if (key.startsWith("child_first_name")) {
				if (key.equalsIgnoreCase("child_first_name1")) {
					genderKey = "child_gender1";
					dobKey = "child_date_of_birth1";
				}
				else {
					genderKey = "child_gender" + key.substring(16);
					dobKey = "child_date_of_birth" + key.substring(16);
				}
			}
			gender = eform.get(genderKey);
			dob = eform.get(dobKey);
			if (depLastName && depLastName.trim()) {
				def Dependant dep = new Dependant();
				dep.setLastName(depLastName.trim())
				if (depFirstName && depFirstName.trim() )
				{
					dep.setFirstName(depFirstName.trim());
				}
				if (depMiddleName && depMiddleName.trim() )
				{
					dep.setMiddleName(depMiddleName.trim());
				}
				dep.setGender(new Integer(gender));
				dep.setBirthdate(DateUtil.convertDate(dob));
				dep.setRelationship("CHILD");
				deps.add(dep);
			}
		}
	}
	deps
}

def mergeAndSaveCaseDependents(eFormDependents)
{
	logger.debug("Saving " + eFormDependents.size() + " new deps");
	oldDeps = services.hibernateTemplate.getAll(Dependant.class, "select b from " + Dependant.class.getCanonicalName() + " as b where b.applicationCase = ?", currentCase);
	logger.debug("Removing " + oldDeps.size() + " old deps");
	oldDeps.each { services.hibernateTemplate.delete(it); };
	deps = new ArrayList<Dependant>();
	for(Dependant dep: eFormDependents)
	{
		if (null != dep)
		{
			dep.setApplicationCase(currentCase);
			services.hibernateTemplate.save(dep);
			deps.add(dep);
			logger.debug("Dep " + dep.getId() + " saved.");
		}
	}
	currentCase.setDependants(deps);
}