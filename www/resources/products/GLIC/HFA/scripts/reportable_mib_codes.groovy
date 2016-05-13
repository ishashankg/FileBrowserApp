import org.apache.commons.lang.StringUtils;
import com.stepsoln.core.util.DateUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.stepsoln.core.db.Lookup;
import com.stepsoln.core.db.cases.Case;
import com.stepsoln.core.db.cases.Applicant;
import com.stepsoln.core.db.enums.APPLICANT_TYPE;
import com.stepsoln.core.db.cases.Case.CASE_SOURCE;
import com.stepsoln.core.db.party.Address;
import com.stepsoln.core.eform.EForm;
import com.stepsoln.core.eform.Question;
import com.stepsoln.core.eform.Section;
import com.stepsoln.core.eform.Page;
import com.stepsoln.core.eform.Group;
import com.stepsoln.core.hibernate.StepHibernateTemplate;
import com.stepsoln.core.eform.EFormAnswers;
import org.slf4j.Logger;
import com.stepsoln.core.db.party.Agent;
import com.stepsoln.core.db.agency.Agency;
import com.stepsoln.core.db.agency.CarrierProducerAppointment;
import com.stepsoln.core.db.security.SecurityUser;
import com.stepsoln.core.db.security.SecurityOu;
import com.stepsoln.core.db.services.util.EmployeeUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import java.util.ArrayList;
import java.util.List;
import com.stepsoln.core.db.uw.CaseUwReportableMibCode;
import com.stepsoln.core.db.uw.CaseUwOccupationAvocationTravel;
import com.stepsoln.core.db.uw.CaseUwMedicalCondition.DIAGNOSIS_SOURCE;

class RMIBGeneratorHelper {
	org.slf4j.Logger logger;
	Long caseId;
	Long applicantId;
	Long riskCalcId;
	Map<String, EFormAnswers> eforms;
	List<CaseUwOccupationAvocationTravel> oats;

	def AVIATION_LIST = ["PRIVATE PILOT", "STUDENT PILOT", "COMMERCIAL PILOT", "MILITARY PILOT", "INSTRUCTOR PILOT", "TEST PILOT PROTOTYPE"];
	/*motor racing avocations*/
	def G_LIST = ["MOTOR RACING FORMULA 1", "MOTOR RACING FORMULA 1300", "MOTOR RACING FORMULA 3", "MOTOR RACING FORMULA 3000"];
	/*flying avocations*/
	def H_LIST = ["BALLOONING", "SKY DIVING/PARACHUTING", "HANG GLIDING", "BUNGEE JUMPING", "SKY DIVING", "SKYDIVING"];
	/*underwater sports avocations*/
	def J_LIST = ["SCUBA DIVING", "SKIN DIVING", "ARMED FORCES DIVING", "PEARL DIVING", "DREDGING DIVING", "EXPLORATION DIVING", "UNDERWATER HOCKEY", "QUARRY DIVING"];

	public List<CaseUwReportableMibCode> generateReportableMibCodesOutOfOat()
	{
		List<CaseUwReportableMibCode> result = new ArrayList<CaseUwReportableMibCode>();
		List<CaseUwOccupationAvocationTravel> aviationOats = new ArrayList<CaseUwOccupationAvocationTravel>();
		List<CaseUwOccupationAvocationTravel> gListOats = new ArrayList<CaseUwOccupationAvocationTravel>();
		List<CaseUwOccupationAvocationTravel> hListOats = new ArrayList<CaseUwOccupationAvocationTravel>();
		List<CaseUwOccupationAvocationTravel> jListOats = new ArrayList<CaseUwOccupationAvocationTravel>();
		List<CaseUwOccupationAvocationTravel> notClassifiedOats = new ArrayList<CaseUwOccupationAvocationTravel>();
		for (CaseUwOccupationAvocationTravel oat : oats)
		{
			logger.debug("testing [" + oat.getConditionValue() + "]");
			if (AVIATION_LIST.contains(oat.getConditionValue()))
			{
				aviationOats.add(oat);
			}else
			if (G_LIST.contains(oat.getConditionValue()))
			{
				gListOats.add(oat);
			}else
			if (H_LIST.contains(oat.getConditionValue()))
			{
				hListOats.add(oat);
			}else
			if (J_LIST.contains(oat.getConditionValue()))
			{
				jListOats.add(oat);
			}else
			{
				notClassifiedOats.add(oat);
			}
			logger.debug("aviationOats: " + aviationOats.size()+", gListOats:" + gListOats.size()+", hListOats:" + hListOats.size()+", jListOats:" + jListOats.size()+", notClassifiedOats:" + notClassifiedOats.size());
		}

		/*aviation question answered*/
		/*if (eforms.REFLEXIVE.m9a == "Y")*/
		if (aviationOats.size()>0)
		{
			CaseUwReportableMibCode rmibc = new CaseUwReportableMibCode(caseId, applicantId, riskCalcId, "054#E#",
					"AVIATION", null);
			result.add(rmibc);
			logger.debug("Added AVIATION: {}", rmibc);
		}
		if (gListOats.size()>0)
		{
			CaseUwReportableMibCode rmibc = new CaseUwReportableMibCode(caseId, applicantId, riskCalcId, "054GE#",
					"Motor Vehicle Racing", null);
			result.add(rmibc);
			logger.debug("Added MVR(G): {}", rmibc);
		}
		if (hListOats.size()>0)
		{
			String hDescription='';
			for (CaseUwOccupationAvocationTravel oath : hListOats)
			{
				if (hDescription.indexOf(oath.getConditionValue())<0)
				{
					hDescription += (hDescription.length()>0?", ":"") + oath.getConditionValue();
				}
				logger.debug("oat: [{}], hDescr: [{}]", oath.getConditionValue(), hDescription);
			}
			logger.debug("hDescription="+hDescription);
			CaseUwReportableMibCode rmibc = new CaseUwReportableMibCode(caseId, applicantId, riskCalcId, "054HE#",
					hDescription, null);
			result.add(rmibc);
			logger.debug("Added AIR(H): {}", rmibc);
		}
		if (jListOats.size()>0)
		{
			CaseUwReportableMibCode rmibc = new CaseUwReportableMibCode(caseId, applicantId, riskCalcId, "054JE#",
					"Underwater Sports (scuba)", null);
			result.add(rmibc);
			logger.debug("Added UNDERWATER(J): {}", rmibc);
		}
		if (notClassifiedOats.size()>0)
		{
			CaseUwReportableMibCode rmibc = new CaseUwReportableMibCode(caseId, applicantId, riskCalcId, "054KE#",
					"Other", null);
			result.add(rmibc);
			logger.debug("Added OTHER(K): {}", rmibc);
		}
		return result;
	}
}